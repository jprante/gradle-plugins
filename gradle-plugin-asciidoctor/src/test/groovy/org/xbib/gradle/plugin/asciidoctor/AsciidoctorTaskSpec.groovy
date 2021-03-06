package org.xbib.gradle.plugin.asciidoctor

import org.asciidoctor.SafeMode
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Ignore
import spock.lang.Specification

class AsciidoctorTaskSpec extends Specification {

    private static final String ASCIIDOCTOR = 'asciidoctor'

    private static final String ASCIIDOC_RESOURCES_DIR = 'build/resources/test/src/asciidoc'

    private static final String ASCIIDOC_BUILD_DIR = 'build/asciidoc'

    private static final String ASCIIDOC_SAMPLE_FILE = 'sample.asciidoc'

    private static final String ASCIIDOC_SAMPLE2_FILE = 'subdir/sample2.ad'

    private static final String ASCIIDOC_INVALID_FILE = 'subdir/_include.adoc'

    private static final DOCINFO_FILE_PATTERN = ~/^(.+\-)?docinfo(-footer)?\.[^.]+$/

    Project project
    
    AsciidoctorProxy mockAsciidoctor
    
    ResourceCopyProxy mockCopyProxy
    
    File testRootDir
    
    File srcDir
    
    File outDir
    
    ByteArrayOutputStream systemOut

    def setup() {
        project = ProjectBuilder.builder().withName('test').build()
        project.configurations.create(ASCIIDOCTOR)
        mockAsciidoctor = Mock(AsciidoctorProxy)
        mockCopyProxy = Mock(ResourceCopyProxy)
        testRootDir = new File('.')
        srcDir = new File(testRootDir, ASCIIDOC_RESOURCES_DIR).absoluteFile
        outDir = new File(project.projectDir, ASCIIDOC_BUILD_DIR)
        systemOut = new ByteArrayOutputStream()
        System.out = new PrintStream(systemOut)
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of options via method"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                options eruby : 'erb'
                options eruby : 'erubis'
                options doctype : 'book', toc : 'right'
            }

        then:
            ! systemOut.toString().contains('deprecated')
            task.options['eruby'] == 'erubis'
            task.options['doctype'] == 'book'
            task.options['toc'] == 'right'
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of options via assignment"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                options = [eruby : 'erb', toc : 'right']
                options = [eruby : 'erubis', doctype : 'book']
            }

        then:
            ! systemOut.toString().contains('deprecated')
            task.options['eruby'] == 'erubis'
            task.options['doctype'] == 'book'
            !task.options.containsKey('toc')
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of attributes via method (Map variant)"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                attributes 'source-highlighter': 'foo'
                attributes 'source-highlighter': 'coderay'
                attributes idprefix : '$', idseparator : '-'
            }

        then:
            ! systemOut.toString().contains('deprecated')
            task.attributes['source-highlighter'] == 'coderay'
            task.attributes['idprefix'] == '$'
            task.attributes['idseparator'] == '-'
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of attributes via method (List variant)"() {
        when:
        Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
            attributes(['source-highlighter=foo', 'source-highlighter=coderay', 'idprefix=$', 'idseparator=-'])
        }

        then:
        ! systemOut.toString().contains('deprecated')
        task.attributes['source-highlighter'] == 'coderay'
        task.attributes['idprefix'] == '$'
        task.attributes['idseparator'] == '-'
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of attributes via method (String variant)"() {
        when:
        Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
            attributes 'source-highlighter=foo source-highlighter=coderay idprefix=$ idseparator=-'
        }

        then:
        ! systemOut.toString().contains('deprecated')
        task.attributes['source-highlighter'] == 'coderay'
        task.attributes['idprefix'] == '$'
        task.attributes['idseparator'] == '-'
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of attributes via assignment"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                attributes = ['source-highlighter': 'foo',idprefix : '$']
                attributes = ['source-highlighter': 'coderay', idseparator : '-']
            }

        then:
            ! systemOut.toString().contains('deprecated')
            task.attributes['source-highlighter'] == 'coderay'
            task.attributes['idseparator'] == '-'
            !task.attributes.containsKey('idprefix')
    }

    @SuppressWarnings('MethodName')
    def "Mixing attributes with options, should produce a warning, but updates should be appended"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                options eruby : 'erubis', attributes : ['source-highlighter': 'foo',idprefix : '$']
                options doctype: 'book', attributes : [idseparator : '-' ]
            }

        then:
            !task.attributes.containsKey('attributes')
            task.attributes['source-highlighter'] == 'foo'
            task.attributes['idseparator'] == '-'
            task.attributes['idprefix'] == '$'
            task.options['eruby'] == 'erubis'
            task.options['doctype'] == 'book'
            // @Ignore('Wrong sysout capture')
            // systemOut.toString().contains('Attributes found in options.')
    }

    @SuppressWarnings('MethodName')
    def "Mixing attributes with options with assignment, should produce a warning, and attributes will be replaced"() {
        when:
            Map tmpStore = [ eruby : 'erubis', attributes : ['source-highlighter': 'foo',idprefix : '$'] ]
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                options = tmpStore
                options = [ doctype: 'book', attributes : [idseparator : '-' ] ]
            }

        then:
            !task.attributes.containsKey('attributes')
            task.attributes['idseparator'] == '-'
            !task.attributes.containsKey('source-highlighter')
            !task.attributes.containsKey('idprefix')
            !task.options.containsKey('eruby')
            task.options['doctype'] == 'book'
            // @Ignore('Wrong sysout capture')
            // systemOut.toString().contains('Attributes found in options.')
    }

    @SuppressWarnings('MethodName')
    def "Mixing string legacy form of attributes with options with assignment, should produce a warning, and attributes will be replaced"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                options = [ doctype: 'book', attributes : 'toc=right source-highlighter=coderay toc-title=Table\\ of\\ Contents' ]
            }

        then:
            task.options['doctype'] == 'book'
            !task.attributes.containsKey('attributes')
            task.attributes['toc'] == 'right'
            task.attributes['source-highlighter'] == 'coderay'
            task.attributes['toc-title'] == 'Table of Contents'
            // @Ignore('Wrong sysout capture')
            // systemOut.toString().contains('Attributes found in options.')
    }

    @SuppressWarnings('MethodName')
    def "Mixing list legacy form of attributes with options with assignment, should produce a warning, and attributes will be replaced"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                options = [ doctype: 'book', attributes : [
                            'toc=right',
                            'source-highlighter=coderay',
                            'toc-title=Table of Contents'
                            ]]
            }

        then:
            task.options['doctype'] == 'book'
            !task.attributes.containsKey('attributes')
            task.attributes['toc'] == 'right'
            task.attributes['source-highlighter'] == 'coderay'
            task.attributes['toc-title'] == 'Table of Contents'
            // @Ignore('Wrong sysout capture')
            // systemOut.toString().contains('Attributes found in options.')
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of backends via method"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                backends 'foo','bar'
                backends 'pdf'
            }

        then:
            ! systemOut.toString().contains('deprecated')
            task.backends.contains('pdf')
            task.backends.contains('foo')
            task.backends.contains('bar')
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of backends via assignment"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                backends = ['pdf']
                backends = ['foo','bar']
            }

        then:
            ! systemOut.toString().contains('deprecated')
            !task.backends.contains('pdf')
            task.backends.contains('foo')
            task.backends.contains('bar')
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of requires via method"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                requires 'slim','tilt'
                requires 'asciidoctor-pdf'
            }

        then:
            ! systemOut.toString().contains('deprecated')
            task.requires.contains('asciidoctor-pdf')
            task.requires.contains('tilt')
            task.requires.contains('slim')
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of requires via assignment"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                requires = ['asciidoctor-pdf']
                requires = ['slim','tilt']
            }

        then:
            ! systemOut.toString().contains('deprecated')
            !task.requires.contains('asciidoctor-pdf')
            task.requires.contains('tilt')
            task.requires.contains('slim')
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of sourceDir via method"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                sourceDir project.projectDir
            }

        then:
            ! systemOut.toString().contains('deprecated')
            task.getSourceDir().absolutePath == project.projectDir.absolutePath
            task.sourceDir.absolutePath == project.projectDir.absolutePath
    }


    @SuppressWarnings('MethodName')
    def "When setting sourceDir via assignment"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                sourceDir = project.projectDir
            }

        then:
            task.getSourceDir().absolutePath == project.projectDir.absolutePath
            task.sourceDir.absolutePath == project.projectDir.absolutePath

    }

    @SuppressWarnings('MethodName')
    def "When setting sourceDir via setSourceDir"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                setSourceDir project.projectDir
            }

        then:
            task.getSourceDir().absolutePath == project.projectDir.absolutePath
            task.sourceDir.absolutePath == project.projectDir.absolutePath
            !systemOut.toString().contains('deprecated')
    }

    @SuppressWarnings('MethodName')
    def "Allow setting of gemPath via method"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                gemPath project.projectDir
            }

        then:
            ! systemOut.toString().contains('deprecated')
            task.asGemPath() == project.projectDir.absolutePath
    }

    @SuppressWarnings('MethodName')
    def "When setting gemPath via assignment"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                gemPath = project.projectDir
            }

        then:
            task.asGemPath() == project.projectDir.absolutePath
            ! systemOut.toString().contains('deprecated')
    }

    @SuppressWarnings('MethodName')
    def "When setting gemPath via setGemPath"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                setGemPath project.projectDir
            }

        then:
            task.asGemPath() == project.projectDir.absolutePath
            ! systemOut.toString().contains('deprecated')
    }

    @SuppressWarnings('MethodName')
    def "sourceDocumentNames should resolve descendant files of sourceDir if supplied as relatives"() {
        when: "I specify two files relative to sourceDir,including one in a subfoler"
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                sourceDir srcDir
                sourceDocumentNames = [ASCIIDOC_SAMPLE_FILE, ASCIIDOC_SAMPLE2_FILE]
            }
            def fileCollection = task.sourceDocumentNames

        then: "both files should be in collection, but any other files found in folder should be excluded"
            fileCollection.contains(new File(srcDir,ASCIIDOC_SAMPLE_FILE).canonicalFile)
            fileCollection.contains(new File(srcDir,ASCIIDOC_SAMPLE2_FILE).canonicalFile)
            !fileCollection.contains(new File(srcDir,'sample-docinfo.xml').canonicalFile)
            fileCollection.files.size() == 2
    }

    @SuppressWarnings('MethodName')
    def "sourceDocumentNames should resolve descendant files of sourceDir even if given as absolute files"() {
        given:
            File sample1 = new File(srcDir,ASCIIDOC_SAMPLE_FILE).absoluteFile
            File sample2 = new File(srcDir,ASCIIDOC_SAMPLE2_FILE).absoluteFile

        when: "I specify two absolute path files, that are descendents of sourceDit"
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                sourceDir srcDir
                sourceDocumentNames = [sample1,sample2]
            }
            def fileCollection = task.sourceDocumentNames

        then: "both files should be in collection, but any other files found in folder should be excluded"
            fileCollection.contains(new File(srcDir,ASCIIDOC_SAMPLE_FILE).canonicalFile)
            fileCollection.contains(new File(srcDir,ASCIIDOC_SAMPLE2_FILE).canonicalFile)
            !fileCollection.contains(new File(srcDir,'sample-docinfo.xml').canonicalFile)
            fileCollection.files.size() == 2
    }

    @SuppressWarnings('MethodName')
    def "sourceDocumentNames should not resolve files that are not descendants of sourceDir"() {
        given:
            File sample1 = new File(project.projectDir,ASCIIDOC_SAMPLE_FILE).absoluteFile

        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                sourceDir srcDir
                sourceDocumentNames = [sample1]
            }
            def fileCollection = task.sourceDocumentNames

        then:
            fileCollection.files.size() == 0
    }

    @SuppressWarnings('MethodName')
    @SuppressWarnings('DuplicateNumberLiteral')
    def "Add asciidoctor task with multiple backends"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                backends AsciidoctorBackend.DOCBOOK.id, AsciidoctorBackend.HTML5.id
            }

            task.processAsciidocSources()
        then:
            2 * mockAsciidoctor.convertFile(_, { Map map -> map.backend == AsciidoctorBackend.DOCBOOK.id})
            2 * mockAsciidoctor.convertFile(_, { Map map -> map.backend == AsciidoctorBackend.HTML5.id})
    }

    @SuppressWarnings('MethodName')
    @SuppressWarnings('DuplicateNumberLiteral')
    def "Adds asciidoctor task with multiple backends and single backend"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                backends = [AsciidoctorBackend.DOCBOOK.id, AsciidoctorBackend.HTML5.id]
                backend = AsciidoctorBackend.DOCBOOK5.id
            }

            task.processAsciidocSources()
        then:
            2 * mockAsciidoctor.convertFile(_, { Map map -> map.backend == AsciidoctorBackend.DOCBOOK.id})
            2 * mockAsciidoctor.convertFile(_, { Map map -> map.backend == AsciidoctorBackend.HTML5.id})
            // @Ignore('Wrong sysout capture')
            // systemOut.toString().contains('Using `backend` and `backends` together will result in `backend` being ignored.')
    }

    @SuppressWarnings('MethodName')
    @SuppressWarnings('DuplicateNumberLiteral')
    def "Adds asciidoctor task with supported backend"() {
        expect:
            project.tasks.findByName(ASCIIDOCTOR) == null
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir srcDir
                outputDir = outDir
            }

            task.processAsciidocSources()
        then:
            2 * mockAsciidoctor.convertFile(_, { Map map -> map.backend == AsciidoctorBackend.HTML5.id})
            ! systemOut.toString().contains('deprecated')
    }

    @SuppressWarnings('MethodName')
    @SuppressWarnings('DuplicateNumberLiteral')
    def "Using setBackend should output a warning"() {
        expect:
            project.tasks.findByName(ASCIIDOCTOR) == null
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                backend = AsciidoctorBackend.DOCBOOK.id
            }

            task.processAsciidocSources()
        then:
            2 * mockAsciidoctor.convertFile(_, { Map map -> map.backend == AsciidoctorBackend.DOCBOOK.id})
    }

    @SuppressWarnings('MethodName')
    def "Adds asciidoctor task throws exception"() {
        expect:
            project.tasks.findByName(ASCIIDOCTOR) == null
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
            }

            task.processAsciidocSources()
        then:
            mockAsciidoctor.convertFile(_, _) >> { throw new IllegalArgumentException() }
            thrown(GradleException)
    }

    @SuppressWarnings('MethodName')
    def "Processes a single document given a value for sourceDocumentName"() {
        expect:
            project.tasks.findByName(ASCIIDOCTOR) == null
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sourceDocumentName = new File(srcDir, ASCIIDOC_SAMPLE_FILE)
            }

            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(_, _)
    }


    @Ignore('Wrong sysout capture')
    @SuppressWarnings('MethodName')
    def "Output warning when a sourceDocumentName was given"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sourceDocumentName = new File(srcDir, ASCIIDOC_SAMPLE_FILE)
            }
        when:
            task.processAsciidocSources()
        then:
            systemOut.toString().contains('setSourceDocumentName is deprecated')
    }

    @Ignore('Wrong sysout capture')
    @SuppressWarnings('MethodName')
    def "Output error when sourceDocumentName and sourceDocumentNames are given"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sourceDocumentName = new File(srcDir, ASCIIDOC_SAMPLE_FILE)
                sourceDocumentNames = new File(srcDir, ASCIIDOC_SAMPLE_FILE)
            }
        when:
            task.processAsciidocSources()
        then:
            systemOut.toString().contains('setSourceDocumentNames is deprecated')
    }

    @SuppressWarnings('MethodName')
    def "Source documents in directories end up in the corresponding output directory"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                separateOutputDirs = false
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE2_FILE), { it.to_dir == new File(task.outputDir, 'subdir').absolutePath })
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), { it.to_dir == task.outputDir.absolutePath })
        and:
            0 * mockAsciidoctor.convertFile(_, _)
    }

    @SuppressWarnings('MethodName')
    def "Should support String value for attributes option"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sourceDocumentName = new File(srcDir, ASCIIDOC_SAMPLE_FILE)
                options = [
                  attributes: 'toc=right source-highlighter=coderay'
                ]
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(_, _)
    }

    @SuppressWarnings('MethodName')
    @SuppressWarnings('DuplicateStringLiteral')
    def "Should support GString value for attributes option"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sourceDocumentName = new File(srcDir, ASCIIDOC_SAMPLE_FILE)
                def attrs = 'toc=right source-highlighter=coderay'
                options = [
                  attributes: "$attrs"
                ]
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(_, _)
    }

    @SuppressWarnings('MethodName')
    @SuppressWarnings('DuplicateStringLiteral')
    def "Should support List value for attributes option"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sourceDocumentName = new File(srcDir, ASCIIDOC_SAMPLE_FILE)
                def highlighter = 'coderay'
                options = [
                  attributes: ['toc=right', "source-highlighter=$highlighter"]
                ]
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(_, _)
    }

    @SuppressWarnings('MethodName')
    def "Throws exception when attributes embedded in options is an unsupported type"() {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sourceDocumentName = new File(srcDir, ASCIIDOC_SAMPLE_FILE)
                options = [
                  attributes: 23
                ]
            }
            task.processAsciidocSources()
        then:
            thrown(org.gradle.api.internal.tasks.DefaultTaskContainer$TaskCreationException)
    }

    @SuppressWarnings('MethodName')
    def "Setting baseDir results in the correct value being sent to Asciidoctor"() {
        given:
            File basedir = new File(testRootDir, 'my_base_dir')
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                baseDir = basedir
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), { it.base_dir == basedir.absolutePath })
    }

    @SuppressWarnings('MethodName')
    def "Omitting a value for baseDir results in sending the dir of the processed file"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE),
                    { it.base_dir == new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE).getParentFile().absolutePath })
    }

    @SuppressWarnings('MethodName')
    def "Setting baseDir to null results in no value being sent to Asciidoctor"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                baseDir = null
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), { !it.base_dir })
    }

    @SuppressWarnings('MethodName')
    def "Safe mode option is equal to level of SafeMode.UNSAFE by default"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), {
                it.safe == SafeMode.UNSAFE.level
            })
    }

    @SuppressWarnings('MethodName')
    def "Safe mode configuration option as integer is honored"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                options = [
                    safe: SafeMode.SERVER.level
                ]
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), {
                it.safe == SafeMode.SERVER.level
            })
    }

    @SuppressWarnings('MethodName')
    def "Safe mode configuration option as string is honored"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                options = [
                    safe: 'server'
                ]
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), {
                it.safe == SafeMode.SERVER.level
            })
    }

    @SuppressWarnings('MethodName')
    def "Safe mode configuration option as enum is honored"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                options = [
                    safe: SafeMode.SERVER
                ]
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), {
                it.safe == SafeMode.SERVER.level
            })
    }

    @SuppressWarnings('MethodName')
    def "Attributes projectdir and rootdir are always set to relative dirs of the processed file"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), {
                it.attributes.projectdir == AsciidoctorUtils.getRelativePath(project.projectDir, new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE).getParentFile())
                it.attributes.rootdir == AsciidoctorUtils.getRelativePath(project.rootDir, new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE).getParentFile())
            })
    }

    @SuppressWarnings('MethodName')
    def "Docinfo files are not copied to target directory"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), _)
            !outDir.listFiles({ !it.directory && !(it.name =~ DOCINFO_FILE_PATTERN) } as FileFilter)
    }

    @SuppressWarnings('MethodName')
    def "Project coordinates are set automatically as attributes"() {
        given:
            project.version = '1.0.0-SNAPSHOT'
            project.group = 'com.acme'
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), {
                it.attributes.'project-name' == 'test' &&
                it.attributes.'project-group' == 'com.acme' &&
                it.attributes.'project-version' == '1.0.0-SNAPSHOT'
            })
    }

    @SuppressWarnings('MethodName')
    def "Override project coordinates with explicit attributes"() {
        given:
            project.version = '1.0.0-SNAPSHOT'
            project.group = 'com.acme'
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                options = [
                   attributes: [
                        'project-name': 'awesome',
                        'project-group': 'unicorns',
                        'project-version': '1.0.0.Final'
                    ]
                ]
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), {
                it.attributes.'project-name' == 'awesome' &&
                    it.attributes.'project-group' == 'unicorns' &&
                    it.attributes.'project-version' == '1.0.0.Final'
            })
    }

    @SuppressWarnings('MethodName')
    def "Should support a single source document if a name is given"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sourceDocumentName = new File(ASCIIDOC_RESOURCES_DIR, ASCIIDOC_SAMPLE_FILE)
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE),_ )
    }

    @SuppressWarnings('MethodName')
    def "Should support multiple source documents if names are given"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sources {
                  include ASCIIDOC_SAMPLE_FILE,ASCIIDOC_SAMPLE2_FILE
                }
            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE),_ )
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE2_FILE),_ )
    }

    @SuppressWarnings('MethodName')
    def "Should throw exception if the file sourceDocumentName starts with underscore"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir = srcDir
                outputDir = outDir
                sourceDocumentName = new File(srcDir, ASCIIDOC_INVALID_FILE)
            }
        when:
            task.processAsciidocSources()
        then:
            0 * mockAsciidoctor.convertFile(_, _)
            thrown(GradleException)
    }

    @SuppressWarnings('MethodName')
    def "Should throw exception if a file in sourceDocumentNames starts with underscore"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir srcDir
                outputDir outDir
                setSourceDocumentNames ASCIIDOC_INVALID_FILE
            }
        when:
            task.processAsciidocSources()
        then:
            0 * mockAsciidoctor.convertFile(_, _)
            thrown(GradleException)
    }

    def "When 'resources' not specified, then copy all images to backend"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy

                sourceDir srcDir
                outputDir outDir
                backends AsciidoctorBackend.HTML5.id

                sources {
                    include ASCIIDOC_SAMPLE_FILE
                }

            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockCopyProxy.copy(_, _)
    }

    def "When 'resources' not specified and more than one backend, then copy all images to every backend"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy

                sourceDir srcDir
                outputDir outDir
                backends AsciidoctorBackend.HTML5.id,AsciidoctorBackend.DOCBOOK.id

                sources {
                    include ASCIIDOC_SAMPLE_FILE
                }

            }
        when:
            task.processAsciidocSources()
        then:
            1 * mockCopyProxy.copy( new File(outDir,AsciidoctorBackend.HTML5.id) , _)
            1 * mockCopyProxy.copy( new File(outDir,AsciidoctorBackend.DOCBOOK.id) , _)
    }

    def "When 'resources' are specified, then copy according to all patterns"() {
        given:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy

                sourceDir srcDir
                outputDir outDir
                backends AsciidoctorBackend.HTML5.id,AsciidoctorBackend.DOCBOOK.id

                sources {
                    include ASCIIDOC_SAMPLE_FILE
                }

                resources {
                    from (sourceDir) {
                        include 'images/**'
                    }
                }
            }

        when:
            task.processAsciidocSources()
        then:
            1 * mockCopyProxy.copy( new File(outDir,AsciidoctorBackend.HTML5.id) , _)
            1 * mockCopyProxy.copy( new File(outDir,AsciidoctorBackend.DOCBOOK.id) , _)
    }

    def "sanity test for default configuration" () {
        when:
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
            }

        then:
            task.sourceDir.absolutePath.replace('\\', '/').endsWith('src/docs/asciidoc')
            task.outputDir.absolutePath.replace('\\', '/').endsWith('build/asciidoc')
    }

    def "set output dir to something else than the default" () {
        given:
        project.version = '1.0.0-SNAPSHOT'
        project.group = 'com.acme'

        Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
            asciidoctor = mockAsciidoctor
            resourceCopyProxy = mockCopyProxy
            sourceDir = srcDir
        }
        project.buildDir =  '/tmp/testbuild/asciidoctor'

        when:
        task.processAsciidocSources()

        then: 'the html files must be in testbuild/asciidoctor '
        task.outputDir.absolutePath.replace('\\', '/').contains('testbuild/asciidoctor')
        2 * mockAsciidoctor.convertFile(_, { it.to_dir.startsWith(project.buildDir.absolutePath) })
    }

    def "Files in the resources copyspec should be recognised as input files" () {
        given:
            File imagesDir = new File(outDir,'images')
            File imageFile = new File(imagesDir,'fake.txt')
            imagesDir.mkdirs()
            imageFile.text = 'foo'

            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy

                sourceDir srcDir
                outputDir "${outDir}/foo"
                backends AsciidoctorBackend.HTML5.id

                sources {
                    include ASCIIDOC_SAMPLE_FILE
                }

                resources {
                    from (outDir) {
                        include 'images/**'
                    }
                }
            }

        when:
            project.evaluate()

        then:
            task.inputs.files.contains(project.file("${srcDir}/sample.asciidoc"))
            task.inputs.files.contains(project.file("${imagesDir}/fake.txt"))
    }

    def "GroovyStrings in options and attributes are converted into Strings" () {
        given:
            String variable = 'bar'
            Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
                options = [
                    template_dirs: ["${project.projectDir}/templates/haml"]
                ]
                attributes = [
                    foo: "${variable}"
                ]
                asciidoctor = mockAsciidoctor
                resourceCopyProxy = mockCopyProxy
                sourceDir srcDir
                outputDir = outDir
            }

        when:
            task.processAsciidocSources()

        then:
            1 * mockAsciidoctor.convertFile(new File(task.sourceDir, ASCIIDOC_SAMPLE_FILE), { Map props ->
                props.template_dirs*.class == [String] &&
                props.attributes.foo.class == String
            })
    }

    @SuppressWarnings('MethodName')
    def "Should require libraries"() {
        expect:
        project.tasks.findByName(ASCIIDOCTOR) == null
        when:
        Task task = project.tasks.create(name: ASCIIDOCTOR, type: AsciidoctorTask) {
            asciidoctor = mockAsciidoctor
            resourceCopyProxy = mockCopyProxy
            sourceDir = srcDir
            outputDir = outDir
            sourceDocumentName = new File(srcDir, ASCIIDOC_SAMPLE_FILE)
            requires 'asciidoctor-pdf'
        }

        task.processAsciidocSources()
        then:
        1 * mockAsciidoctor.requireLibrary("asciidoctor-pdf")
    }

}
