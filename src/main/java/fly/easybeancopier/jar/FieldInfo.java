package fly.easybeancopier.jar;

import javax.lang.model.type.TypeMirror;

/**
 * @author FlyInWind
 */
public class FieldInfo {
    private String sourceName;
    private TypeMirror sourceType;
    private String targetName;
    private TypeMirror targetType;

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public TypeMirror getSourceType() {
        return sourceType;
    }

    public void setSourceType(TypeMirror sourceType) {
        this.sourceType = sourceType;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public TypeMirror getTargetType() {
        return targetType;
    }

    public void setTargetType(TypeMirror targetType) {
        this.targetType = targetType;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }
}