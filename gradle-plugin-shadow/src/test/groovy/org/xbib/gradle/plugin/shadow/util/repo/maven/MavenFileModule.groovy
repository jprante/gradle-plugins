package org.xbib.gradle.plugin.shadow.util.repo.maven

import org.xbib.gradle.plugin.shadow.util.file.TestFile

class MavenFileModule extends AbstractMavenModule {
    private boolean uniqueSnapshots = true;

    MavenFileModule(TestFile moduleDir, String groupId, String artifactId, String version) {
        super(moduleDir, groupId, artifactId, version)
    }

    boolean getUniqueSnapshots() {
        return uniqueSnapshots
    }

    MavenModule withNonUniqueSnapshots() {
        uniqueSnapshots = false;
        return this;
    }

    @Override
    String getMetaDataFileContent() {
        """
<metadata>
  <!-- ${getArtifactContent()} -->
  <groupId>$groupId</groupId>
  <artifactId>$artifactId</artifactId>
  <version>$version</version>
  <versioning>
    <snapshot>
      <timestamp>${timestampFormat.format(publishTimestamp)}</timestamp>
      <buildNumber>$publishCount</buildNumber>
    </snapshot>
    <lastUpdated>${updateFormat.format(publishTimestamp)}</lastUpdated>
  </versioning>
</metadata>
"""
    }

    @Override
    protected onPublish(TestFile file) {
        sha1File(file)
        md5File(file)
    }

    @Override
    protected boolean publishesMetaDataFile() {
        uniqueSnapshots && version.endsWith("-SNAPSHOT")
    }

    @Override
    protected boolean publishesHashFiles() {
        true
    }
}
