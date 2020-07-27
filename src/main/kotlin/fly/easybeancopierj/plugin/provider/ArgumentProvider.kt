package fly.easybeancopier.plugin.provider

import com.intellij.openapi.util.RecursionGuard
import com.intellij.openapi.util.RecursionManager
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.augment.PsiAugmentProvider
import com.intellij.psi.impl.source.PsiExtensibleClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import java.util.*
import kotlin.collections.ArrayList

class ArgumentProvider : PsiAugmentProvider() {

    override fun <Psi : PsiElement> getAugments(element: PsiElement, type: Class<Psi>): MutableList<Psi> {
        if (type != PsiMethod::class.java || element !is PsiExtensibleClass) {
            return Collections.emptyList()
        }
        if (!element.isValid) {
            return Collections.emptyList()
        }
//        val psiClass: PsiClass = element
        // Skip processing of Annotation and Interface
        if (element.isAnnotationType || element.isInterface) {
            return Collections.emptyList()
        }
        val classCopierCachedValueProvider = MethodCopierCachedValueProvider(type as Class<Psi>, element)
        return CachedValuesManager.getCachedValue(element, classCopierCachedValueProvider) ?: Collections.emptyList()
    }

    companion object {
//        private class ClassCopierCachedValueProvider<Psi : PsiElement>(type: Class<Psi>, psiClass: PsiClass)
//            : CopierCachedValueProvider<Psi>(type, psiClass, ourGuard) {
//            companion object {
//                val ourGuard: RecursionGuard<PsiClass> = RecursionManager.createGuard("copier.augment.class")
//            }
//        }

        private class MethodCopierCachedValueProvider<Psi : PsiElement>(type: Class<Psi>, psiClass: PsiClass)
            : CopierCachedValueProvider<Psi>(type, psiClass, ourGuard) {
            companion object {
                val ourGuard: RecursionGuard<PsiClass> = RecursionManager.createGuard("copier.augment.method")
            }
        }

        private abstract class CopierCachedValueProvider<Psi : PsiElement>(
                private val type: Class<Psi>,
                private val psiClass: PsiClass,
                private val recursionGuard: RecursionGuard<PsiClass>)
            : CachedValueProvider<MutableList<Psi>> {

            override fun compute(): CachedValueProvider.Result<MutableList<Psi>>? {
                return recursionGuard.doPreventingRecursion(psiClass, true, this::computeIntern)
            }

            private fun computeIntern(): CachedValueProvider.Result<MutableList<Psi>> {
                val result = getPsis(psiClass, type)
                return CachedValueProvider.Result.create(result, psiClass)
            }
        }

        private fun <Psi : PsiElement> getPsis(psiClass: PsiClass, type: Class<Psi>): MutableList<Psi> {
            val result = ArrayList<Psi>()
            val copierProcessors = ProcessorProvider.getInstance(psiClass.project).getCopierProcessors(type)
            for (processor in copierProcessors) {
                val generatedElement = processor.process(psiClass)
                for (psiElement in generatedElement) {
                    @Suppress("UNCHECKED_CAST")
                    result.add(psiElement as Psi)
                }
            }
            return result
        }
    }
}