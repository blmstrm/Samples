package com.devwebsphere.wxsutils.multijob;
//
//This sample program is provided AS IS and may be used, executed, copied and
//modified without royalty payment by customer (a) for its own instruction and
//study, (b) in order to develop applications designed to run with an IBM
//WebSphere product, either for customer's own internal use or for redistribution
//by customer, as part of such an application, in customer's own products. "
//
//5724-J34 (C) COPYRIGHT International Business Machines Corp. 2009
//All Rights Reserved * Licensed Materials - Property of IBM
//


import java.io.Serializable;

import com.ibm.websphere.objectgrid.Session;

public interface SinglePartTask<V,R> extends Serializable
{
	/**
	 * This is called on the grid side to process the next block within this partition
	 * @param sess The session to the local partition primary
	 * @return the next block of processing information for the user
	 */
	V process(Session sess);
	
	/**
	 * This tests of the result has any data
	 * @return
	 */
	boolean isResultEmpty(R result);
}
