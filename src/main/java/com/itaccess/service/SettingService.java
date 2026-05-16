package com.itaccess.service;

import com.itaccess.entity.Setting;
import com.itaccess.repository.SettingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SettingService {
    
    private final SettingRepository settingRepository;
    
    @Transactional
    public String getSecretKey() {
        return settingRepository.findByKey("SECRET_KEY")
                .map(Setting::getValue)
                .orElseGet(this::generateNewSecretKey);
    }
    
    @Transactional
    public String generateNewSecretKey() {
        String newKey = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        
        Setting setting = Setting.builder()
                .key("SECRET_KEY")
                .value(newKey)
                .build();
        
        settingRepository.save(setting);
        return newKey;
    }
    
    @Transactional
    public void refreshSecretKey() {
        generateNewSecretKey();
    }
}