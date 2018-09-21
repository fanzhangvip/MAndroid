package com.me94me.resource_lifecycle;

import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

public class OnceLiveData extends LiveData<Integer> {

    public void observe(LifecycleOwner owner){
        FinishActivityObserver observer = new FinishActivityObserver(owner);
        observe(owner,observer);
    }

    /**
     * 关闭Activity
     */
    public void finishActivity(){
        postValue(0);
    }

    class FinishActivityObserver implements Observer<Integer> {
        LifecycleOwner owner;
        FinishActivityObserver(LifecycleOwner owner) {
            this.owner = owner;
        }
        @Override
        public void onChanged(Integer integer) {
            if(owner instanceof AppCompatActivity){
                ((AppCompatActivity)owner).finish();
            }
        }
    }

    /**
     * 关闭Activity的listener
     * @return View.OnClickListener
     */
    public View.OnClickListener finishActivityListener(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishActivity();
            }
        };
    }
}
