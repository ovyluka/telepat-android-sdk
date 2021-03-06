package io.telepat.sdk.models;

/**
 * Created by Andrei Marinescu on 11/29/15.
 */
public class UserUpdatePatch {

    public UserUpdatePatch() {}

    public UserUpdatePatch(String fieldName, Object fieldValue) {
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Object getFieldValue() {
        return fieldValue;
    }

    public void setFieldValue(Object fieldValue) {
        this.fieldValue = fieldValue;
    }

    private String fieldName;
    private Object fieldValue;
}
