package uk.gov.moj.cpp.workmanagement.proxy.api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypedValue {
    private Object value;
    private ValueType type;
    private boolean isTransient;
}
