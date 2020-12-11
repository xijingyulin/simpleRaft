package com.sraft.client;

import com.sraft.client.exception.KeyNullException;
import com.sraft.client.exception.UnavailableException;
import com.sraft.client.exception.ValueNullException;

public interface IClientTransaction {

	ActionResult put(String key, String value) throws UnavailableException, KeyNullException, ValueNullException;

	ActionResult update(String key, String value) throws UnavailableException, KeyNullException, ValueNullException;

	ActionResult remove(String key) throws UnavailableException, KeyNullException;

	ActionResult get(String key) throws UnavailableException, KeyNullException;
}
