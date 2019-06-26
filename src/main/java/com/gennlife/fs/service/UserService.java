package com.gennlife.fs.service;

import com.gennlife.fs.configurations.GeneralConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static com.gennlife.fs.configurations.HospitalNames.*;

@Service
public class UserService {

    @SuppressWarnings("DuplicateBranchesInSwitch")
    public boolean isAllowedToAccessPatientPrivacyData(String userId) {
        switch (cfg.hospitalName) {
            case HOSPITAL_DEV:
                return true;
            case HOSPITAL_JSZLYY:
                return "bd0d6b6d-12eb-419f-82cb-dfe50ce4b55e".equals(userId);
            case HOSPITAL_JZYKDXDIFSYY:
                return "8ec80ec4-34f8-4acb-999a-4fc508406bde".equals(userId);
            case HOSPITAL_XAJTDXDYFSYY:
                return true;
            default:
                return false;
        }
    }

    @Autowired
    private GeneralConfiguration cfg;

}
