package com.hmdp.utils;

/**
 * @Author: WereAsh
 * @Date:2024-03-10 22:30
 **/
public interface ILock {
    boolean tryLock(long timeOutSec);
    void unLock();
}
