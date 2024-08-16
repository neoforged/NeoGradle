package net.neoforged.gradle.dsl.common.extensions.subsystems

import net.minecraftforge.gdi.BaseDSLElement
import org.gradle.api.provider.Property

/**
 * Defines the integration settings for NeoGradle.
 */
interface Integration extends BaseDSLElement<Integration> {

    /**
     * @return whether the integration is enabled
     */
    Property<Boolean> getIsEnabled();

    /**
     * @return whether to use Gradle problem reporting
     */
    Property<Boolean> getUseGradleProblemReporting();
}