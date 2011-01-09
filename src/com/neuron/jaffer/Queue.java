/** 
 * Copyright (c) 2003-2007 Stewart Allen <stewart@neuron.com>. All rights reserved.
 * This program is free software. See the 'License' file for details.
 */

package com.neuron.jaffer;

public class Queue
{
	private Element head;
	private Element tail;

	//private int count;
	//private int maxCount;

	private Element createElement(Object obj)
	{
		return new Element(obj);
	}

	public void enqueue(Object obj)
	{
		synchronized (this)
		{
			if (tail == null)
			{
				tail = createElement(obj);
				head = tail;
			}
			else
			{
				tail.next = createElement(obj);
				tail = tail.next;
			}
			/*
			count++;
			if (count > maxCount)
			{
				maxCount = count;
				System.out.println("max queue len = "+maxCount+" : "+this);
			}
			*/
			this.notify();
		}
	}

	public Object dequeue()
	{
		synchronized (this)
		{
			try
			{
				while (head == null)
				{
					this.wait();
				}
			}
			catch (InterruptedException ex)
			{
				return null;
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return null;
			}
			Element element = head;
			if (head.next == null)
			{
				head = null;
				tail = null;
			}
			else
			{
				head = head.next;
			}
			//count--;
			return element.object;
		}
	}

	private class Element
	{
		Object object;
		Element next;

		Element(Object object)
		{
			this.object = object;
		}
	}
}

