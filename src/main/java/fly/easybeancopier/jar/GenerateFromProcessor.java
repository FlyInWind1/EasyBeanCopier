package fly.easybeancopier.jar;

import com.google.auto.service.AutoService;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.model.JavacTypes;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Name;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypesException;
import javax.tools.Diagnostic;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author FlyInWind
 */
@AutoService(Processor.class)
public class GenerateFromProcessor extends AbstractProcessor {
    private JavacTrees trees;
    private JavacProcessingEnvironment jpe;
    private Context context;
    private TreeMaker treeMaker;

    private JavacElements elementUtil;
    private JavacTypes typeUtils;
    //    private Names nameUtils;
    private Messager messager;

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(GenerateFrom.class.getCanonicalName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.trees = JavacTrees.instance(processingEnv);
        this.jpe = (JavacProcessingEnvironment) processingEnv;
        this.context = jpe.getContext();
        this.treeMaker = TreeMaker.instance(context);

        this.elementUtil = jpe.getElementUtils();
        this.typeUtils = jpe.getTypeUtils();
//        this.nameUtils = Names.instance(context);
        this.messager = processingEnv.getMessager();
    }

    private static final String GET_L = "get";
    private static final String SET_L = "set";
    private static final String COPIER = "Copier";
    private static final long PUBLIC_STATIC_FLAG = Flags.PUBLIC | Flags.STATIC;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            //注解的class
            Class<GenerateFrom> easyBeanCopierClass = GenerateFrom.class;
            //遍历被注解的类
            Set<? extends Element> targetElements = roundEnv.getElementsAnnotatedWith(easyBeanCopierClass);
            for (Element targetElement : targetElements) {
                //获取包名，类名
                String targetClassName = targetElement.getSimpleName().toString();
                String copierClassName = targetClassName + COPIER;
                //目标类的tree
                JCTree.JCClassDecl targetClassTree = (JCTree.JCClassDecl) elementUtil.getTree(targetElement);

                //获取目标类的属性列表
                Set<FieldInfo> fieldInfos = new HashSet<>();
                targetElement.getEnclosedElements().forEach(e -> {
                    if (e.getKind() == ElementKind.FIELD) {
                        FieldInfo fieldInfo = new FieldInfo();
                        fieldInfo.setTargetName(e.getSimpleName().toString());
                        fieldInfo.setTargetType(e.asType());
                        fieldInfos.add(fieldInfo);
                    }
                });

                GenerateFrom annotation = targetElement.getAnnotation(easyBeanCopierClass);
                //遍历源class
                Set<Symbol.ClassSymbol> sourceElements = null;
                //可以通过异常获取注解中的value对应的element
                try {
                    @SuppressWarnings("unused")
                    Class<?>[] sourceClasses = annotation.value();
                    // TODO: 2020/2/23 没有异常时获取element
                } catch (MirroredTypesException e) {
                    sourceElements = e.getTypeMirrors().stream().map(t -> (Symbol.ClassSymbol) typeUtils.asElement(t)).collect(Collectors.toSet());
                }
                //遍历源类,生成代码
                for (Symbol.ClassSymbol sourceElement : sourceElements) {
                    //过滤,并设置源类属性信息
                    LinkedList<FieldInfo> infosBackup = new LinkedList<>(fieldInfos);
                    LinkedList<FieldInfo> infos = new LinkedList<>();
                    sourceElement.getEnclosedElements().forEach(e -> {
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

                    JCTree.JCClassDecl classTree = (JCTree.JCClassDecl) trees.getTree(targetElement);

                    Name sourceFiledName = elementUtil.getName("source");
                    JCTree.JCVariableDecl variableDecl = treeMaker.VarDef(
                            treeMaker.Modifiers(Flags.PARAMETER),
                            sourceFiledName,
                            treeMaker.Ident(sourceElement), null);
                    JCTree.JCMethodDecl methodDecl = treeMaker.MethodDef(
                            treeMaker.Modifiers(PUBLIC_STATIC_FLAG),
                            elementUtil.getName("generateFrom"),
                            treeMaker.Type((Type) targetElement.asType()),
                            List.nil(),
                            List.of(variableDecl),
                            List.nil(),
                            createGeneratorBlock((Symbol) targetElement, sourceFiledName, infos),
                            null);
                    classTree.defs = classTree.defs.append(methodDecl);
                }
            }
        } catch (Exception e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            messager.printMessage(Diagnostic.Kind.ERROR, stringWriter.toString());
        }
        return true;
    }

    private JCTree.JCBlock createGeneratorBlock(Symbol targetSymbol, Name sourceFieldName, LinkedList<FieldInfo> infos) {
//        Symbol.VarSymbol v;
        Name fieldName = elementUtil.getName("t");

        ListBuffer<JCTree.JCStatement> statementList = new ListBuffer<>();
        statementList.append(treeMaker.VarDef(
                treeMaker.Modifiers(0),
                fieldName,
                treeMaker.Ident(targetSymbol),
                treeMaker.NewClass(null, null, treeMaker.Ident(targetSymbol), List.nil(), null)));

        infos.forEach(i -> statementList.append(treeMaker.Exec(treeMaker.Apply(
                null,
                treeMaker.Select(treeMaker.Ident(fieldName), setterName(i.getTargetName())),
                List.of(treeMaker.Apply(
                        null,
                        treeMaker.Select(treeMaker.Ident(sourceFieldName), getterName(i.getSourceName())),
                        List.nil()))))));

        statementList.append(treeMaker.Return(
                treeMaker.Ident(fieldName)));
        return treeMaker.Block(0, statementList.toList());
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

    Name setterName(String field) {
        return elementUtil.getName(SET_L + uppercaseFirstChar(field));
    }

    Name getterName(String field) {
        return elementUtil.getName(GET_L + uppercaseFirstChar(field));
    }
}
