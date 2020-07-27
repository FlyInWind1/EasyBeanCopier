package fly.easybeancopier.plugin.provider

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import de.plushnikov.intellij.plugin.processor.Processor
import fly.easybeancopier.plugin.processor.ProcessorManager
import fly.easybeancopier.plugin.processor.clazz.GenerateFromProcessor
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class ProcessorProvider {
    private val processors: MutableMap<String, MutableCollection<Processor>>
    private val typeProcessor: MutableMap<Class<*>, MutableCollection<Processor>>
    private val registeredAnnotationNames: MutableCollection<String>
    private var alreadyInitialized: Boolean = false

    init {
        processors = ConcurrentHashMap()
        typeProcessor = ConcurrentHashMap()
        registeredAnnotationNames = ConcurrentHashMap.newKeySet()
    }

    companion object {
        fun getInstance(project: Project): ProcessorProvider {
            ServiceManager.getService(GenerateFromProcessor::class.java)
            val service = ServiceManager.getService(project, ProcessorProvider::class.java)
            service.checkInitialized()
            return service
        }
    }

    private fun checkInitialized() {
        if (!alreadyInitialized) {
            initProcessor()
            alreadyInitialized = true
        }
    }

    private fun initProcessor() {
        processors.clear()
        typeProcessor.clear()
        registeredAnnotationNames.clear()

        for (processor in ProcessorManager.getProcessor()) {
            val annotationClasses = processor.supportedAnnotationClasses
            for (annotationClass in annotationClasses) {
                putProcessor(processors, annotationClass.name, processor)
                putProcessor(processors, annotationClass.simpleName, processor)
            }
            putProcessor(typeProcessor, processor.supportedClass, processor)
        }

        registeredAnnotationNames.addAll(processors.keys)
    }

    fun getCopierProcessors(supportedClass: Class<out PsiElement>): MutableCollection<Processor> {
        return typeProcessor.computeIfAbsent(supportedClass) { ConcurrentHashMap.newKeySet() }
    }

    fun getProcessor(psiAnnotation: PsiAnnotation): Collection<Processor> {
        val qualifiedName = psiAnnotation.qualifiedName
        return (if (qualifiedName == null) null else processors[qualifiedName])
                ?: Collections.emptySet()
    }

    private fun <K, V> putProcessor(map: MutableMap<K, MutableCollection<V>>, key: K, value: V) {
        val valueList: MutableCollection<V> = map.computeIfAbsent(key, { ConcurrentHashMap.newKeySet() })
        valueList.add(value)
    }
}