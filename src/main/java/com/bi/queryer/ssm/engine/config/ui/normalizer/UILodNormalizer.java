package com.bi.queryer.ssm.engine.config.ui.normalizer;

import com.bi.queryer.ssm.custom.CustomFieldType;
import com.bi.queryer.ssm.engine.config.ui.UIQueryConfigure;
import com.bi.queryer.ssm.engine.config.ui.UIQueryField;

import java.util.List;
import java.util.stream.Collectors;

public class UILodNormalizer extends UIBaseNormalizer{
    public UILodNormalizer(UIQueryConfigure uiQueryConfigure) {
        super(uiQueryConfigure);
    }

    @Override
    public void normalize() {

        List<UIQueryField>  lodFields = uiQueryConfigure.getResult().getMeasures().stream()
                .filter(f-> f.getCustomFieldConfigure() != null)
                .filter(f-> CustomFieldType.isLod(f.getCustomFieldConfigure().getType())).collect(Collectors.toList());

        for (UIQueryField lodField : lodFields){
            lodField.setCode("");
            lodField.setName("");
        }

    }
}
