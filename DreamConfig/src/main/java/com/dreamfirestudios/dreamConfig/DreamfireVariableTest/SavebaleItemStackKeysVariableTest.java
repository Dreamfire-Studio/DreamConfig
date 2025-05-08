package com.dreamfirestudios.dreamConfig.DreamfireVariableTest;

import com.dreamfirestudios.dreamConfig.Enum.SaveabaleItemStackKeys;
import com.dreamfirestudios.dreamCore.DreamfireJava.PulseAutoRegister;
import com.dreamfirestudios.dreamCore.DreamfirePersistentData.PersistentDataTypes;
import com.dreamfirestudios.dreamCore.DreamfireVariable.DreamfireVariableTest;

import java.util.ArrayList;
import java.util.List;

@PulseAutoRegister
public class SavebaleItemStackKeysVariableTest implements DreamfireVariableTest {
    @Override
    public PersistentDataTypes PersistentDataType() { return PersistentDataTypes.STRING; }

    @Override
    public boolean IsType(Object variable) {
        try{
            var test = SaveabaleItemStackKeys.valueOf(variable.toString());
            return true;
        }catch (IllegalArgumentException ignored){ return false; }
    }

    @Override
    public List<Class<?>> ClassTypes() {
        var data = new ArrayList<Class<?>>();
        data.add(SaveabaleItemStackKeys.class);
        data.add(SaveabaleItemStackKeys[].class);
        return data;
    }

    @Override
    public Object SerializeData(Object serializedData) {
        return serializedData.toString();
    }

    @Override
    public Object DeSerializeData(Object serializedData) {
        return SaveabaleItemStackKeys.valueOf(serializedData.toString());
    }

    @Override
    public Object ReturnDefaultValue() { return SaveabaleItemStackKeys.ITEMSTACK; }

    @Override
    public List<String> TabData(List<String> list, String s) {
        var data = new ArrayList<String>();
        for(var x : SaveabaleItemStackKeys.values()) if(x.name().contains(s)) data.add(x.name());
        return data;
    }
}