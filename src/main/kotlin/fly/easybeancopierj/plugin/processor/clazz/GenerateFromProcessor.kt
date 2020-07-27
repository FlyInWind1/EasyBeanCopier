package fly.easybeancopier.plugin.processor.clazz

import com.intellij.psi.*
import com.intellij.psi.util.PsiTypesUtil
import de.plushnikov.intellij.plugin.problem.ProblemBuilder
import de.plushnikov.intellij.plugin.processor.clazz.AbstractClassProcessor
import de.plushnikov.intellij.plugin.psi.LombokLightMethodBuilder
import fly.easybeancopier.jar.GenerateFrom

class GenerateFromProcessor : AbstractClassProcessor(PsiMethod::class.java, GenerateFrom::class.java) {
    companion object {
        const val GENERATE_FROM = "generateFrom"
    }

    override fun validate(psiAnnotation: PsiAnnotation, psiClass: PsiClass, builder: ProblemBuilder): Boolean {
        if (psiClass.isAnnotationType || psiClass.isInterface || psiClass.isEnum) {
            builder.addError("'@%s' is only supported on a class or field type", psiAnnotation.qualifiedName)
            return false
        }
        return true
    }

    override fun generatePsiElements(psiClass: PsiClass, psiAnnotation: PsiAnnotation, target: MutableList<in PsiElement>) {
        val sourceSymbols = psiAnnotation.findAttributeValue("value")
        if (sourceSymbols is PsiArrayInitializerMemberValue) {
            sourceSymbols.children.forEach {
                if (it is PsiClassObjectAccessExpression) {
                    target.add(createGenerateFromMethod(it.operand.type, psiClass))
                }
            }
        } else if (sourceSymbols is PsiClassObjectAccessExpression) {
            target.add(createGenerateFromMethod(sourceSymbols.operand.type, psiClass))
        }
    }

    private fun createGenerateFromMethod(sourceType: PsiType, targetClass: PsiClass): PsiMethod {
        val targetType = PsiTypesUtil.getClassType(targetClass)
        return LombokLightMethodBuilder(targetClass.manager, GENERATE_FROM)
                .withModifier("public static")
                .withMethodReturnType(targetType)
                .withContainingClass(targetClass)
                .withParameter("source", sourceType)
                .withNavigationElement(targetClass)
    }
}