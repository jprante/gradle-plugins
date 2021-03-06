package org.xbib.gradle.plugin.jflex

import jflex.exceptions.GeneratorException
import jflex.generator.LexGenerator
import jflex.logging.Out
import jflex.option.Options
import jflex.skeleton.Skeleton
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.StopActionException
import org.gradle.api.tasks.TaskAction

import java.nio.charset.Charset

@CacheableTask
class JFlexTask extends DefaultTask {

    private final Logger logger = Logging.getLogger(JFlexTask)

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    Iterable<File> source

    @OutputDirectory
    File target

    @Internal
    SourceSet theSourceSet

    @TaskAction
    void generateAndTransformJflex() throws Exception {
        JFlexExtension ext = project.extensions.findByType(JFlexExtension)
        Options.setRootDirectory(ext.rootDirectory ? ext.rootDirectory : new File(""))
        Skeleton.readDefault()
        if (ext.skel) {
            Skeleton.readSkelFile(ext.skel)
        }
        Options.encoding = ext.encoding ? Charset.forName(ext.encoding) : Charset.defaultCharset()
        Options.verbose = ext.verbose
        Options.progress = ext.progress
        Options.unused_warning = ext.unused_warning
        Options.jlex = ext.jlex
        Options.no_minimize = ext.no_minimize
        Options.no_backup = ext.no_backup
        Options.time = ext.time
        Options.dot = ext.dot
        Options.dump = ext.dump
        Options.legacy_dot = ext.legacy_dot
        // hack for writing directly into java source. Not recommended.
        if (ext.writeIntoJavaSrc) {
            if (theSourceSet.java && theSourceSet.java.srcDirs) {
                logger.info "java sources: ${theSourceSet.java.srcDirs}"
                target = theSourceSet.java.srcDirs.first()
                logger.info "switching to first java source directory ${target}"
            } else {
                logger.warn "writing into java source not possible, is empty"
            }
        }
        source.each { file ->
            String pkg = getPackageName(file)
            File fullTarget = new File(target, pkg.replace('.','/'))
            project.mkdir(fullTarget)
            Options.directory = fullTarget
            logger.info "jflex task: source=${file} pkg=${pkg} dir=${target}"
            try {
                new LexGenerator(file).generate()
            } catch (GeneratorException e) {
                Logging.getLogger(JFlexTask).error("JFlex error: ${e.message}", e)
                throw new StopActionException('an error occurred during JFlex code generation')
            }
        }
        if (ext.statistics) {
            Out.statistics()
        }
    }

    static String getPackageName(File file) {
        String string = file.readLines().find { line ->
            line.startsWith('package')
        }
        return string == null ? '' : string.substring(8, string.length() - 1)
    }
}
