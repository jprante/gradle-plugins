package org.xbib.gradle.plugin.docker

import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional

class DockerBuildTask extends Exec {

    @InputDirectory
    File basePath = new File('.')

    @Input
    List<String> buildArgs = []

    @Input
    List<String> instructions = []

    @Optional
    @Input
    String baseImage

    @Optional
    @Input
    String maintainer

    @Input
    boolean enabled = true

    @Input
    String executableName = 'docker'

    @Optional
    @Input
    String registry

    @Input
    String imageName

    @Optional
    @Input
    String tag

    private Dockerfile dockerfile

    DockerBuildTask() {
        File file = new File(project.buildDir, 'docker-' + name)
        if (!file.exists()) {
            file.mkdirs()
        }
        this.dockerfile = new Dockerfile(project, file)
        setWorkingDir(dockerfile.workingDir)
    }

    void dockerfile(Closure closure) {
        dockerfile.with(closure)
    }

    void dockerfile(String path) {
        dockerfile(project.file(path))
    }

    void dockerfile(File baseFile) {
        dockerfile.extendDockerfile(baseFile)
    }

    @Override
    void exec() {
        if (enabled) {
            File file = createDockerFile()
            executeStagingBacklog()
            commandLine buildCommandLine(file)
            super.exec()
        }
    }

    private File createDockerFile() {
        if (!dockerfile.hasBase()) {
            dockerfile.from(baseImage ? baseImage : 'scratch')
        }
        if (maintainer) {
            dockerfile.maintainer(maintainer)
        }
        if (instructions) {
            dockerfile.appendAll(instructions)
        }
        File file = new File(workingDir, 'Dockerfile')
        dockerfile.writeToFile(file)
        logger.info "${dockerfile.instructions}"
        file
    }

    private void executeStagingBacklog() {
        dockerfile.stagingBacklog.each() { closure -> closure() }
    }

    private List<String> buildCommandLine(File dockerfilename) {
        List<String> list = [ executableName, 'build', basePath.path ]
        if (args) {
            list.addAll(args)
        }
        list << '-f' << dockerfilename.absoluteFile.toString()
        String fullImageName = imageName
        if (registry) {
            fullImageName = "${registry}/${imageName}".toString()
        }
        if (tag) {
            fullImageName = "${fullImageName}:${tag}".toString()
        }
        list << '-t' << fullImageName
        buildArgs.each {
            list << '--build-arg' << it
        }
        list
    }
}
