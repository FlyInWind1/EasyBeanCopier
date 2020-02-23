package fly.processor.beancopier;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author FlyInWind
 */
@AutoService(Processor.class)
public class BeanGeneratorProcessor extends AbstractProcessor {
    private Elements elementUtil;
    private Types typeUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(BeanGenerator.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtil = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    private static final String GET_L = "get";
    private static final String SET_L = "set";
    private static final String COPIER = "Copier";

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            //注解的class
            Class<BeanGenerator> processorBeanCopierClass = BeanGenerator.class;
            //遍历被注解的类
            Set<? extends Element> targetElements = roundEnv.getElementsAnnotatedWith(processorBeanCopierClass);
            for (Element targetElement : targetElements) {
                //获取包名，类名
                String targetClassName = targetElement.getSimpleName().toString();
                String copierClassName = targetClassName + COPIER;
                //构造copier
                TypeSpec.Builder classSpecBuilder = TypeSpec
                        .classBuilder(copierClassName)
                        .addModifiers(Modifier.PUBLIC);

                //获取目标类的属性列表
                List<FieldInfo> fieldInfos = new LinkedList<>();
                targetElement.getEnclosedElements().forEach(e -> {
                    if (e.getKind() == ElementKind.FIELD) {
                        FieldInfo fieldInfo = new FieldInfo();
                        fieldInfo.setTargetName(e.getSimpleName().toString());
                        fieldInfo.setTargetType(e.asType());
                        fieldInfos.add(fieldInfo);
                    }
                });

                BeanGenerator annotation = targetElement.getAnnotation(processorBeanCopierClass);
                //遍历源class
                List<Element> sourceElements = null;
                //可以通过异常获取注解中的value对应的element
                try {
                    @SuppressWarnings("unused")
                    Class<?>[] sourceClasses = annotation.value();
                    // TODO: 2020/2/23 没有异常时获取element
                } catch (MirroredTypesException e) {
                    sourceElements = e.getTypeMirrors().stream().map(t -> typeUtils.asElement(t)).collect(Collectors.toList());
                }
                //遍历源类,生成代码
                for (Element sourceElement : sourceElements) {
                    //过滤,并设置源类属性信息
                    List<FieldInfo> infosBackup = new LinkedList<>(fieldInfos);
                    List<FieldInfo> infos = new LinkedList<>();
                    List<? extends Element> sourceElementFields = sourceElement.getEnclosedElements();
                    sourceElementFields.forEach(e -> {
                        if (e.getKind() == ElementKind.FIELD) {
                            for (int i = 0, m = infosBackup.size(); i < m; i++) {
                                FieldInfo fieldInfo = infosBackup.get(i);
                                String fieldName = e.getSimpleName().toString();
                                if (fieldInfo.getTargetName().equals(fieldName)) {
                                    FieldInfo info = infosBackup.remove(i);
                                    info.setSourceName(fieldName);
                                    info.setSourceType(e.asType());
                                    infos.add(info);
                                    break;
                                }
                            }
                        }
                    });
                    //生成代码
                    MethodSpec.Builder methodSpecBuilder = MethodSpec
                            .methodBuilder("generateFrom")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .returns(TypeName.get(targetElement.asType()))
                            .addParameter(TypeName.get(sourceElement.asType()), "source")
                            .addStatement("$1T t = new $1T()", TypeName.get(targetElement.asType()));
                    infos.forEach(i ->
                            methodSpecBuilder.addStatement("t.$N(source.$N())"
                                    , SET_L + uppercaseFirstChar(i.getTargetName())
                                    , GET_L + uppercaseFirstChar(i.getSourceName())));
                    methodSpecBuilder.addStatement("return t");
                    classSpecBuilder.addMethod(methodSpecBuilder.build());
                }
                String targetPackageName = elementUtil.getPackageOf(targetElement).getQualifiedName().toString();
                JavaFile javaFile = JavaFile.builder(targetPackageName + ".copier", classSpecBuilder.build()).build();
                javaFile.writeTo(filer);
            }
        } catch (Exception e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            messager.printMessage(Diagnostic.Kind.ERROR, stringWriter.toString());
        }
        return true;
    }

    static final char A_L = 'a';
    static final char Z_L = 'z';

    /**
     * 首字母大写
     *
     * @param str 字符串
     * @return 首字母大写后
     */
    static String uppercaseFirstChar(String str) {
        char[] chars = str.toCharArray();
        char firstChar = chars[0];
        if (firstChar >= A_L && firstChar <= Z_L) {
            chars[0] -= 32;
        }
        return new String(chars);
    }
}
