package com.gennlife.fs.service;

import com.gennlife.fs.configurations.GeneralConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.gennlife.fs.configurations.HospitalNames.HOSPITAL_DEV;
import static com.gennlife.fs.configurations.HospitalNames.HOSPITAL_JZYKDXDIFSYY;

@Service
public class UserService {

    public boolean isAllowedToAccessPatientPrivacyData(String userId) {
        switch (cfg.hospitalName) {
            case HOSPITAL_DEV:
                return true;
            case HOSPITAL_JZYKDXDIFSYY:
                return "8ec80ec4-34f8-4acb-999a-4fc508406bde".equals(userId);
            default:
                return false;
        }
    }

    @Autowired
    private GeneralConfiguration cfg;

}
