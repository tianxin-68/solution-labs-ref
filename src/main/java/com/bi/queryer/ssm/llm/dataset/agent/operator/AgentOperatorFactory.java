package com.bi.queryer.ssm.llm.dataset.agent.operator;

import com.bi.queryer.ssm.llm.dataset.agent.operator.impl.AgentMidOperator;
import com.bi.queryer.ssm.llm.dataset.agent.operator.impl.BaseAgentOperator;
import com.bi.queryer.ssm.llm.entity.LLMQueryConfig;
import com.bi.queryer.ssm.llm.entity.LLMQueryField;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AgentOperatorFactory {

    public static BaseAgentOperator get(LLMQueryConfig config){

        List<LLMQueryField> metrics = config.getResult().getMetrics();
        List<String> codeList = metrics
                .stream()
                .map(f -> f.getField_code()).collect(Collectors.toList());

        AgentOperatorType type = AgentOperatorType.getByCodeList(codeList);

        //获取计算的原始字段配置
        AgentOperatorContext context = new AgentOperatorContext();
        if(AgentOperatorType.UNKNOWN != type){

            LLMQueryField operatorField =  metrics
                    .stream()
                    .filter(m->m.getField_code().toLowerCase().contains(type.getCode().toLowerCase()))
                    .findAny()
                    .get();

            String regex = "\\(([^)]+)\\)";
            // 创建Pattern对象
            Pattern pattern = Pattern.compile(regex);
            // 创建Matcher对象
            Matcher matcher = pattern.matcher(operatorField.getField_code());

            String atomFieldCode;
            // 检查是否匹配
            if (matcher.find()) {
                atomFieldCode = matcher.group(1);
            } else {
                atomFieldCode = "";
            }

            LLMQueryField atomField =  metrics
                    .stream()
                    .filter(m->m.getField_code().equalsIgnoreCase(atomFieldCode))
                    .findAny()
                    .get();

            context.setOperatorField(operatorField);
            context.setAtomField(atomField);

        }

        switch (type){
            case BI_AGENT_MID:
              return new AgentMidOperator(config,context);
        }

        return new BaseAgentOperator(config,context);
    }
}
