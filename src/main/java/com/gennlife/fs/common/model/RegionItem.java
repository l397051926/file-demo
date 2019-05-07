package com.gennlife.fs.common.model;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by Chenjinfeng on 2016/10/19.
 */
public class RegionItem<K>{
    private static Logger logger= LoggerFactory.getLogger(RegionItem.class);
    K key;
    private boolean maxset=false;
    private boolean minset=false;
    public double getMax() {
        return max;
    }

    private void setMax(double max) {
        this.max = max;
        maxset=true;
    }
    public void update(String record)
    {
        try {
            double recordDouble = Double.parseDouble(record);

            if (!this.maxset || this.getMax() < recordDouble)
                this.setMax(recordDouble);
            if (!this.minset || this.getMin() > recordDouble)
                this.setMin(recordDouble);
        } catch (Exception e) {
            logger.debug(" Double.parseDouble " + record);
        }
    }
    public double getMin() {
        return min;
    }

    private void setMin(double min) {
        this.min = min;
        minset=true;
    }

    double max=Double.MIN_VALUE;
    double min=Double.MAX_VALUE;

    public RegionItem(K key)
    {
        this.key=key;
    }
    public JsonObject toJson()
    {

        JsonObject result =new JsonObject();
        if(!maxset)
            result.addProperty("max","");
        else
            result.addProperty("max",max);
        if(!minset)
            result.addProperty("min","");
        else
            result.addProperty("min",min);
        return result;
    }


    public K getKey() {
        return key;
    }

}
