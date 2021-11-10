package org.xbib.gradle.plugin

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.xbib.rpm.Dependency
import org.xbib.rpm.Directory
import org.xbib.rpm.Link
import org.xbib.rpm.RpmBuilder
import org.xbib.rpm.payload.Directive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

class RpmCopyAction implements CopyAction {

    Project project

    Rpm task

    RpmBuilder builder

    Path tempDir

    RpmCopyAction(Project project, Rpm task) {
        this.project = project
        this.task = task
    }

    @Override
    WorkResult execute(CopyActionProcessingStream copyActionProcessingStream) {
        if (task.enabled) {
            task.setDuplicatesStrategy(DuplicatesStrategy.INCLUDE)
            tempDir = task.getTemporaryDir().toPath()
            builder = createRpm()
            copyActionProcessingStream.process(new StreamAction())
            addOther()
            buildRpm()
        }
        WorkResults.didWork(true)
    }

    RpmBuilder createRpm() {
        RpmBuilder builder = new RpmBuilder()
        builder.setPackage task.packageName, task.packageVersion, task.packageRelease, task.epoch
        builder.setType task.packageType
        builder.setPlatform task.arch, task.os
        builder.setGroup task.packageGroup
        builder.setBuildHost task.buildHost
        builder.setSummary task.summary
        builder.setDescription task.packageDescription
        builder.setLicense task.license
        builder.setPackager task.packager
        builder.setDistribution task.distribution
        builder.setVendor task.vendor
        builder.setUrl task.url
        builder.setPrefixes task.prefixes
        builder.setPrivateKeyId task.getSigningKeyId()
        builder.setPrivateKeyPassphrase task.getSigningKeyPassphrase()
        builder.setPrivateKeyRing task.getSigningKeyRing()
        builder.setPrivateKeyHashAlgo task.getSigningKeyHashAlgo()
        builder.setSourceRpm(task.sourcePackage)
        builder.setPreInstall task.preInstallFile?.text ?: task.preInstall
        builder.setPostInstall task.postInstallFile?.text ?: task.postInstall
        builder.setPreUninstall task.preUninstallFile?.text ?: task.preUninstall
        builder.setPostUninstall task.postUninstallFile?.text ?: task.postUninstall
        builder.setPreTrans task.preTransFile?.text ?: task.preTrans
        builder.setPostTrans task.postTransFile?.text ?: task.postTrans
        builder.setPreInstall prepareScripts(task, task.preInstallCommands)
        builder.setPostInstall prepareScripts(task, task.postInstallCommands)
        builder.setPreUninstall prepareScripts(task, task.preUninstallCommands)
        builder.setPostUninstall prepareScripts(task, task.postUninstallCommands)
        builder.setPreTrans prepareScripts(task, task.preTransCommands)
        builder.setPostTrans prepareScripts(task, task.postTransCommands)
        if (task.changeLogFile) {
            builder.addChangelog task.changeLogFile?.toPath()
        }
        if (task.changeLog) {
            builder.addChangelog(task.changeLog)
        }
        builder
    }

    void addOther() {
        for (Link link : task.links) {
            builder.addLink(link.path, link.target, link.permissions)
        }
        for (Dependency dep : task.dependencies) {
            builder.addDependency(dep.packageName, dep.flags, dep.version)
        }
        for (Dependency obsolete: task.obsoletes) {
            builder.addObsoletes(obsolete.packageName, obsolete.flags, obsolete.version)
        }
        for (Dependency conflict : task.conflicts) {
            builder.addConflicts(conflict.packageName, conflict.flags, conflict.version)
        }
        for (Dependency provides : task.provides) {
            builder.addProvides(provides.packageName, provides.flags, provides.version)
        }
        for (Directory directory : task.directories) {
            String user = directory.user ? directory.user : task.user
            String group = directory.group ? directory.group : task.group
            builder.addDirectory(directory.path, directory.permissions, null, user, group, directory.addParents)
        }
    }

    void buildRpm() {
        Path path = task.archiveFile.get().asFile.toPath()
        Files.createDirectories(path.parent)
        Files.newByteChannel(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING).withCloseable { ch ->
            builder.build(ch)
        }
    }

    private class StreamAction implements CopyActionProcessingStreamAction {

        @Override
        void processFile(FileCopyDetailsInternal fileCopyDetailsInternal) {
            boolean addParents = task.addParentDirs != null ? task.addParentDirs : task.addParentDirs
            Path path = extractPath(tempDir, fileCopyDetailsInternal)
            String p = "/${fileCopyDetailsInternal.path}"
            if (Files.isSymbolicLink(path)) {
                builder.addLink(p, Files.readSymbolicLink(path).toFile().path, -1)
            } else if (!fileCopyDetailsInternal.isDirectory()) {
                int mode = fileCopyDetailsInternal.mode
                int dirmode = -1
                EnumSet<Directive> directive = makeDirective(task.fileType)
                String user = task.user ?: task.user
                String group = task.group ?: task.group
                builder.addFile(p, path, mode, dirmode, directive, user, group, addParents)
            }
        }
    }

    private static Path extractPath(Path tempDir, FileCopyDetails fileDetails) {
        Path path
        try {
            path = fileDetails.file.toPath()
        } catch (UnsupportedOperationException e) {
            path = tempDir.resolve(fileDetails.path)
            fileDetails.copyTo(path.toFile())
        }
        path
    }

    private static EnumSet<Directive> makeDirective(List<String> strings) {
        EnumSet<Directive> set = EnumSet.of(Directive.NONE)
        for (String string : strings) {
            set.add(Directive.valueOf(string.toUpperCase(Locale.ROOT)))
        }
        set
    }

    private static String prepareScripts(Rpm task, List<Object> scripts) {
        if (scripts != null && !scripts.isEmpty()) {
            List<Object> list = []
            def stdDefines = standardScriptDefines(task)
            if (stdDefines) {
                list.add(stdDefines)
            }
            if (task.commonCommands) {
                list.addAll(task.commonCommands)
            }
            list.addAll(scripts)
            return concatAndRemoveShebangLines(list)
        } else {
            return null
        }
    }

    private static String standardScriptDefines(Rpm task) {
        String.format(" RPM_ARCH=%s \n RPM_OS=%s \n RPM_PACKAGE_NAME=%s \n RPM_PACKAGE_VERSION=%s \n RPM_PACKAGE_RELEASE=%s \n\n",
                task.arch?.toString()?.toLowerCase()?:'',
                task.os?.toString()?.toLowerCase()?: '',
                task.packageName,
                task.packageVersion,
                task.packageRelease)
    }

    private static String concatAndRemoveShebangLines(Collection<Object> scripts) {
        String shebang
        StringBuilder result = new StringBuilder()
        scripts.each { script ->
            script?.eachLine { line ->
                if (line.matches('^#!.*$')) {
                    if (!shebang) {
                        shebang = line
                    } else if (line != shebang) {
                        throw new IllegalArgumentException("mismatching #! script lines")
                    }
                } else {
                    result.append line
                    result.append "\n"
                }
            }
        }
        if (shebang) {
            result.insert(0, shebang + "\n")
        }
        result.toString()
    }
}
