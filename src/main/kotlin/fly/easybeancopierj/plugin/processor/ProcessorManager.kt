package fly.easybeancopier.plugin.processor

import com.intellij.openapi.components.ServiceManager
import fly.easybeancopier.plugin.processor.clazz.GenerateFromProcessor

class ProcessorManager {
    companion object {
        fun getProcessor(): List<GenerateFromProcessor> {
            return listOf(
                    ServiceManager.getService(GenerateFromProcessor::class.java)
            )
        }
    }
}