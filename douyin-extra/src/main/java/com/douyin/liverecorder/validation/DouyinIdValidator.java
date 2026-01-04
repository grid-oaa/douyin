package com.douyin.liverecorder.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 抖音号验证器
 * 验证抖音号格式：字母和数字组合
 */
public class DouyinIdValidator implements ConstraintValidator<ValidDouyinId, String> {

    private static final int MIN_LENGTH = 1;
    private static final int MAX_LENGTH = 50;
    
    @Override
    public void initialize(ValidDouyinId constraintAnnotation) {
        // 初始化方法，可以从注解中获取配置参数
    }

    @Override
    public boolean isValid(String douyinId, ConstraintValidatorContext context) {
        // null值由@NotNull等其他注解处理
        if (douyinId == null) {
            return false;
        }
        
        return isValidDouyinId(douyinId);
    }
    
    /**
     * 验证抖音号格式
     * 规则：
     * 1. 不能为空
     * 2. 只能包含字母和数字
     * 3. 长度在1-50之间
     * 
     * @param douyinId 抖音号
     * @return 是否有效
     */
    public static boolean isValidDouyinId(String douyinId) {
        if (douyinId == null || douyinId.isEmpty()) {
            return false;
        }
        
        if (douyinId.length() < MIN_LENGTH || douyinId.length() > MAX_LENGTH) {
            return false;
        }
        
        // 只允许字母和数字
        return douyinId.matches("^[a-zA-Z0-9]+$");
    }
}
