package com.gennlife.fs.common.model;

import java.util.Collection;
import java.util.TreeSet;

import static com.gennlife.fs.common.utils.StringUtil.getTimesEvent;

public class EventValue{
        TreeSet<String> hasTimes=new TreeSet<>();
        TreeSet<String> notHasTimes=new TreeSet<>();
        public EventValue(Collection<String> events)
        {
            for(String key:events)
            {
                String tmp= getTimesEvent(key);
                if(tmp!=null)//带次数的
                {
                    if(!notHasTimes.contains(tmp)) {
                        hasTimes.add(key);
                        notHasTimes.add(tmp);
                    }
                    else
                    {//取最大的
                        for(String tmpkey:hasTimes)
                        {
                            if(getTimesEvent(tmpkey).equals(tmp))
                            {
                                hasTimes.remove(tmpkey);
                                hasTimes.add(key);
                                break;
                            }
                        }
                    }
                }
                else notHasTimes.add(key);
            }

        }

        public TreeSet<String> getHasTimes() {
            return hasTimes;
        }

        public TreeSet<String> getNotHasTimes() {
            return notHasTimes;
        }
    }