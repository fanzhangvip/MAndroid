package com.me94me.resource_lifecycle.livedata;

/**
 * 公开SetValue()和PostValue()
 *
 * @param <T> The type of data hold by this instance
 */
public class MutableLiveData<T> extends LiveData<T> {
    @Override
    public void postValue(T value) {
        super.postValue(value);
    }

    @Override
    public void setValue(T value) {
        super.setValue(value);
    }
}
