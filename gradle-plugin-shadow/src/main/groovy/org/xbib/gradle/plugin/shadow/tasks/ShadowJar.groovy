package org.xbib.gradle.plugin.shadow.tasks

import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.file.copy.CopySpecResolver
import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.bundling.ZipEntryCompression
import org.gradle.api.Action
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.util.PatternSet
import org.gradle.internal.Factory
import org.gradle.api.tasks.util.internal.PatternSets
import org.xbib.gradle.plugin.shadow.ShadowStats
import org.xbib.gradle.plugin.shadow.internal.DefaultDependencyFilter
import org.xbib.gradle.plugin.shadow.internal.DefaultZipCompressor
import org.xbib.gradle.plugin.shadow.internal.DependencyFilter
import org.xbib.gradle.plugin.shadow.internal.UnusedTracker
import org.xbib.gradle.plugin.shadow.internal.ZipCompressor
import org.xbib.gradle.plugin.shadow.relocation.Relocator
import org.xbib.gradle.plugin.shadow.relocation.SimpleRelocator
import org.xbib.gradle.plugin.shadow.transformers.AppendingTransformer
import org.xbib.gradle.plugin.shadow.transformers.GroovyExtensionModuleTransformer
import org.xbib.gradle.plugin.shadow.transformers.ServiceFileTransformer
import org.xbib.gradle.plugin.shadow.transformers.Transformer
import org.xbib.gradle.plugin.shadow.zip.ZipOutputStream

import java.util.concurrent.Callable

class ShadowJar extends Jar implements ShadowSpec {

    private List<Transformer> transformers

    private List<Relocator> relocators

    private List<Configuration> configurations

    private DependencyFilter dependencyFilter

    private boolean minimizeJar

    private DependencyFilter dependencyFilterForMinimize

    private ShadowStats shadowStats

    ShadowJar() {
        super()
        dependencyFilter = new DefaultDependencyFilter(getProject())
        dependencyFilterForMinimize = new DefaultDependencyFilter(getProject())
        setManifest(new DefaultInheritManifest(getServices().get(FileResolver)))
        transformers = []
        relocators = []
        configurations = []
        shadowStats = new ShadowStats()
        setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
    }

    @Override
    ShadowJar minimize() {
        minimizeJar = true
        this
    }

    @Override
    ShadowJar minimize(Action<DependencyFilter> c) {
        minimize()
        if (c != null) {
            c.execute(dependencyFilterForMinimize)
        }
        return this
    }

    @Override
    @Internal
    ShadowStats getStats() {
        shadowStats
    }

    @Override
    InheritManifest getManifest() {
        (InheritManifest) super.getManifest()
    }

    @Override
    protected CopyAction createCopyAction() {
        DocumentationRegistry documentationRegistry = getServices().get(DocumentationRegistry)
        FileCollection toMinimize = dependencyFilterForMinimize.resolve(configurations)
        UnusedTracker unusedTracker = UnusedTracker.forProject(getProject(), toMinimize)
        CopySpecResolver copySpecResolver = mainSpec.buildRootResolver()
        Factory<PatternSet> patternSetFactory = PatternSets.getNonCachingPatternSetFactory()
        PatternSet patternSet = patternSetFactory.create()
        patternSet.setCaseSensitive(copySpecResolver.caseSensitive)
        patternSet.include(copySpecResolver.allIncludes)
        patternSet.includeSpecs(copySpecResolver.allIncludeSpecs)
        patternSet.exclude(copySpecResolver.allExcludes)
        patternSet.excludeSpecs(copySpecResolver.allExcludeSpecs)
        new ShadowCopyAction(getLogger(), getArchiveFile().get().getAsFile(), getInternalCompressor(), documentationRegistry,
                this.getMetadataCharset(), transformers, relocators, patternSet, shadowStats,
                isPreserveFileTimestamps(), minimizeJar, unusedTracker)
    }

    @Internal
    protected ZipCompressor getInternalCompressor() {
        switch (getEntryCompression()) {
            case ZipEntryCompression.DEFLATED:
                return new DefaultZipCompressor(isZip64(), ZipOutputStream.DEFLATED)
            case ZipEntryCompression.STORED:
                return new DefaultZipCompressor(isZip64(), ZipOutputStream.STORED)
            default:
                throw new IllegalArgumentException(String.format("unknown compression type %s", entryCompression))
        }
    }

    @TaskAction
    protected void copy() {
        from(getIncludedDependencies())
        super.copy()
        getLogger().info(shadowStats.toString())
    }

    @InputFiles
    FileCollection getIncludedDependencies() {
        getProject().files(new Callable<FileCollection>() {

            @Override
            FileCollection call() throws Exception {
                return dependencyFilter.resolve(configurations)
            }
        })
    }

    /**
     * Configure inclusion/exclusion of module & project dependencies into uber jar.
     *
     * @param c the configuration of the filter
     * @return this
     */
    @Override
    ShadowJar dependencies(Action<DependencyFilter> c) {
        if (c != null) {
            c.execute(dependencyFilter)
        }
        this
    }

    /**
     * Add a Transformer instance for modifying JAR resources and configure.
     *
     * @param clazz the transformer to add. Must have a no-arg constructor
     * @return this
     */
    @Override
    ShadowJar transform(Class<? extends Transformer> clazz)
            throws InstantiationException, IllegalAccessException {
        transform(clazz, null)
    }

    /**
     * Add a Transformer instance for modifying JAR resources and configure.
     *
     * @param clazz the transformer class to add. Must have no-arg constructor
     * @param c the configuration for the transformer
     * @return this
     */
    @Override
    ShadowJar transform(Class<? extends Transformer> clazz, Action<? extends Transformer> c)
            throws InstantiationException, IllegalAccessException {
        Transformer transformer = clazz.getDeclaredConstructor().newInstance()
        if (c != null) {
            c.execute(transformer)
        }
        transformers.add(transformer)
        this
    }

    /**
     * Add a preconfigured transformer instance.
     *
     * @param transformer the transformer instance to add
     * @return this
     */
    @Override
    ShadowJar transform(Transformer transformer) {
        transformers.add(transformer)
        this
    }

    /**
     * Syntactic sugar for merging service files in JARs.
     *
     * @return this
     */
    @Override
    ShadowJar mergeServiceFiles() {
        try {
            transform(ServiceFileTransformer.class)
        } catch (IllegalAccessException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        } catch (InstantiationException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        }
        this
    }

    /**
     * Syntactic sugar for merging service files in JARs.
     *
     * @return this
     */
    @Override
    ShadowJar mergeServiceFiles(String rootPath) {
        try {
            transform(ServiceFileTransformer.class, new Action<ServiceFileTransformer>() {

                @Override
                void execute(ServiceFileTransformer serviceFileTransformer) {
                    serviceFileTransformer.setPath(rootPath)
                }
            })
        } catch (IllegalAccessException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        } catch (InstantiationException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        }
        this
    }

    /**
     * Syntactic sugar for merging service files in JARs.
     *
     * @return this
     */
    @Override
    ShadowJar mergeServiceFiles(Action<ServiceFileTransformer> configureClosure) {
        try {
            transform(ServiceFileTransformer.class, configureClosure)
        } catch (IllegalAccessException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        } catch (InstantiationException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        }
        this
    }

    /**
     * Syntactic sugar for merging Groovy extension module descriptor files in JARs
     *
     * @return this
     */
    @Override
    ShadowJar mergeGroovyExtensionModules() {
        try {
            transform(GroovyExtensionModuleTransformer.class)
        } catch (IllegalAccessException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        } catch (InstantiationException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        }
        this
    }

    /**
     * Syntactic sugar for merging service files in JARs
     *
     * @return this
     */
    @Override
    ShadowJar append(String resourcePath) {
        try {
            transform(AppendingTransformer.class, new Action<AppendingTransformer>() {
                @Override
                void execute(AppendingTransformer transformer) {
                    transformer.setResource(resourcePath)
                }
            })
        } catch (IllegalAccessException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        } catch (InstantiationException e) {
            getLogger().log(LogLevel.ERROR, e.getMessage() as String, e)
        }
        return this
    }

    /**
     * Add a class relocator that maps each class in the pattern to the provided destination.
     *
     * @param pattern the source pattern to relocate
     * @param destination the destination package
     * @return this
     */
    @Override
    ShadowJar relocate(String pattern, String destination) {
        relocate(pattern, destination, null)
    }

    /**
     * Add a class relocator that maps each class in the pattern to the provided destination.
     *
     * @param pattern the source pattern to relocate
     * @param destination the destination package
     * @param configure the configuration of the relocator
     * @return this
     */
    @Override
    ShadowJar relocate(String pattern, String destination, Action<SimpleRelocator> configure) {
        SimpleRelocator relocator = new SimpleRelocator(pattern, destination, new ArrayList<String>(), new ArrayList<String>());
        if (configure != null) {
            configure.execute(relocator)
        }
        relocators.add(relocator)
        this
    }

    /**
     * Add a relocator instance.
     *
     * @param relocator the relocator instance to add
     * @return this
     */
    @Override
    ShadowJar relocate(Relocator relocator) {
        relocators.add(relocator)
        this
    }

    /**
     * Add a relocator of the provided class.
     *
     * @param relocatorClass the relocator class to add. Must have a no-arg constructor.
     * @return this
     */
    @Override
    ShadowJar relocate(Class<? extends Relocator> relocatorClass)
            throws InstantiationException, IllegalAccessException {
        relocate(relocatorClass, null)
    }

    /**
     * Add a relocator of the provided class and configure.
     *
     * @param relocatorClass the relocator class to add. Must have a no-arg constructor
     * @param configure the configuration for the relocator
     * @return this
     */
    @Override
    ShadowJar relocate(Class<? extends Relocator> relocatorClass, Action<? extends Relocator> configure)
            throws InstantiationException, IllegalAccessException {
        Relocator relocator = relocatorClass.getDeclaredConstructor().newInstance()
        if (configure != null) {
            configure.execute(relocator)
        }
        relocators.add(relocator)
        this
    }

    @Internal
    List<Transformer> getTransformers() {
        return this.transformers
    }

    void setTransformers(List<Transformer> transformers) {
        this.transformers = transformers
    }

    @Internal
    List<Relocator> getRelocators() {
        return this.relocators
    }

    void setRelocators(List<Relocator> relocators) {
        this.relocators = relocators
    }

    @InputFiles @Optional
    List<Configuration> getConfigurations() {
        this.configurations
    }

    void setConfigurations(List<Configuration> configurations) {
        this.configurations = configurations
    }

    @Internal
    DependencyFilter getDependencyFilter() {
        return this.dependencyFilter
    }

    void setDependencyFilter(DependencyFilter filter) {
        this.dependencyFilter = filter
    }
}
