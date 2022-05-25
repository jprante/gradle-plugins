package org.xbib.gradle.plugin.shadow

import org.xbib.gradle.plugin.shadow.util.PluginSpecification

class ConfigureShadowPluginSpec extends PluginSpecification {

    def "auto relocate plugin dependencies"() {
        given:
        buildFile << """
            task relocateShadowJar(type: org.xbib.gradle.plugin.shadow.tasks.ConfigureShadowRelocation) {
                target = tasks.shadowJar
            }
            tasks.shadowJar.dependsOn tasks.relocateShadowJar

            dependencies {
               implementation 'junit:junit:3.8.2'
            }
        """.stripIndent()

        when:
        runner.withArguments('shadowJar', '-s').build()

        then:
        contains(output, [
                'META-INF/MANIFEST.MF',
                'shadow/junit/textui/ResultPrinter.class',
                'shadow/junit/textui/TestRunner.class',
                'shadow/junit/framework/Assert.class',
                'shadow/junit/framework/AssertionFailedError.class',
                'shadow/junit/framework/ComparisonCompactor.class',
                'shadow/junit/framework/ComparisonFailure.class',
                'shadow/junit/framework/Protectable.class',
                'shadow/junit/framework/Test.class',
                'shadow/junit/framework/TestCase.class',
                'shadow/junit/framework/TestFailure.class',
                'shadow/junit/framework/TestListener.class',
                'shadow/junit/framework/TestResult$1.class',
                'shadow/junit/framework/TestResult.class',
                'shadow/junit/framework/TestSuite$1.class',
                'shadow/junit/framework/TestSuite.class'
        ])
    }
}
