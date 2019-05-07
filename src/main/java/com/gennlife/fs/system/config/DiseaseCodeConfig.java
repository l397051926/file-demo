package com.gennlife.fs.system.config;

/**
 *
 * 疾病分类
 * */
public class DiseaseCodeConfig {
    private  String[] diseaseTypeName;
    public DiseaseCodeConfig()
    {
        if(diseaseTypeName==null)
        {
            diseaseTypeName =new String[]{
                    "某些传染病和寄生虫病",
                    "肿瘤",
                    "血液及造血器官疾病和涉及免疫机制的某些疾患",
                    "内分泌、营养和代谢疾病",
                    "精神和行为障碍",
                    "神经系统疾病",
                    "眼和附器疾病",
                    "耳和乳突疾病",
                    "循环系统疾病",
                    "呼吸系统疾病",
                    "消化系统疾病",
                    "皮肤和皮下组织疾病",
                    "肌肉骨骼系统和结缔组织疾病",
                    "泌尿生殖系统疾病",
                    "妊娠、分娩和产褥期",
                    "起源于围生期的某些情况",
                    "先天性畸形、变形和染色体异常",
                    "症状、体征和临床与实验室异常所见，不可归类在他处者",
                    "损伤、中毒和外因的某些其他后果",
                    "疾病和死亡的外因",
                    "影响健康状态和与保健机构接触的因素",
                    "用于特殊目的的编码"
                    };
        }
    }
    public String decode(String diseasecode)
    {
        try {
            int index=-1;//第几类
            diseasecode = diseasecode.toUpperCase();
            char type = diseasecode.charAt(0);
            int codenum = Integer.parseInt(diseasecode.substring(1, 3));
            if(codenum>=0)
            switch (type)
            {
                case 'A':
                case 'B':index=1;
                    break;
                case 'C':index=2;
                    break;
                case 'D':if(codenum<=48)index=2;
                    else if(codenum>=50 && codenum<=89)index=3;
                    break;
                case 'E':if(codenum<=90) index=4;
                    break;
                case 'F':index=5;
                    break;
                case 'G':index=6;
                    break;
                case 'H':if(codenum<=59) index=7;
                    else if(60<=codenum && codenum<=95) index=8;
                    break;
                case 'I':index=9;
                    break;
                case 'J':index=10;
                    break;
                case 'K':if(codenum<=93)
                    index=11;
                    break;
                case 'L':
                    index=12;
                    break;
                case 'M':index=13;
                    break;
                case 'N':index=14;
                    break;
                case 'O':index=15;
                    break;
                case 'P':if(codenum<=96)
                    index=16;
                    break;
                case 'Q':index=17;
                    break;
                case 'R':index=18;
                    break;
                case 'S':index=19;
                    break;
                case 'T':if(codenum<=98)
                    index=19;
                    break;
                case 'V':if(codenum>=1) index=20;
                    break;
                case 'W':
                case 'X':index=20;break;
                case 'Y':if(codenum<=98) index=20;
                    break;
                case 'Z':index=21;
                    break;
                case 'U':if(codenum<=85)index=22;
                    break;
                default: index=-1;
            }
            if(index == -1)
             return null;
            else
                return diseaseTypeName[index-1];
        }
        catch (Exception e)
        {
            return null;
        }

    }
}
