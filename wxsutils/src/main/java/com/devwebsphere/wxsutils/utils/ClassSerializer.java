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
package com.devwebsphere.wxsutils.utils;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.devwebsphere.wxsutils.WXSMapOfLists.BulkPushItem;
import com.ibm.websphere.objectgrid.ObjectGridRuntimeException;

/**
 * This is a helper class to track a set of classes and serialize/deserialize
 * them in a more efficient form than with write object.
 * @author bnewport
 *
 */
public class ClassSerializer
{
	static Logger logger = Logger.getLogger(ClassSerializer.class.getName());

	final static byte STRING = 1;
	final static byte LIST = 2;
	final static byte MAP = 3;
	final static byte BULKLISTITEM = 4;
	final static byte BYTE = 5;
	final static byte INTEGER = 6;
	final static byte LONG = 7;
	final static byte DATE = 8;
	final static byte TIMESTAMP = 9;
	final static byte FLOAT = 10;
	final static byte DOUBLE = 11;
	
	byte nextCode = 20;
	
	/**
	 * Goes from Class to byte
	 */
	Map<Class<? extends Externalizable>, Byte> class2IdMap = new HashMap<Class<? extends Externalizable>, Byte>();
	/**
	 * Goes from byte to Class
	 */
	Map<Byte, Class<? extends Externalizable>> id2ClassMap = new HashMap<Byte, Class<? extends Externalizable>>();

	public ClassSerializer()
	{
	}
	
	/**
	 * Add another class to this serializer. Any classes added will be serialized in a more efficient
	 * format.
	 * @param list The list of classes to add
	 */
	public void storeClass(Class<? extends Externalizable>... list)
	{
		for(Class<? extends Externalizable> c : list)
		{
			if(class2IdMap.containsKey(c))
			{
				logger.log(Level.SEVERE, "duplicate class registered " + c.toString() + " in Serializer " + this.getClass().getSimpleName());
				throw new ObjectGridRuntimeException("Duplicate class registered: " + c.toString());
			}
			class2IdMap.put(c, nextCode);
			id2ClassMap.put(nextCode, c);
			nextCode++;
			// watch for if too many are registered
			if(nextCode == Byte.MAX_VALUE)
			{
				logger.log(Level.SEVERE, "Too many classes registered with " + this.getClass().getSimpleName());
				throw new ObjectGridRuntimeException("Too many classes registered with " + this.getClass().getSimpleName());
			}
		}
	}

	public void writeNullableObject(ObjectOutput out, Object f)
		throws IOException
	{
		if(f != null)
		{
			out.writeBoolean(true);
			writeObject(out, f);
		}
		else
			out.writeBoolean(false);
	}

	public Object readNullableObject(ObjectInput in)
		throws IOException
	{
		if(in.readBoolean())
			return readObject(in);
		else
			return null;
	}
	
	/**
	 * This writes the non-null object to the stream. If the object is an optimized one
	 * then a byte is written identifying the object, followed by the externalize form of 
	 * the object.
	 * @param out
	 * @param f
	 * @throws IOException
	 */
	public void writeObject(ObjectOutput out, Object f)
	throws IOException
	{
		if(f instanceof String)
		{
			out.writeByte(STRING);
			out.writeUTF((String)f);
		} else
		if(f instanceof List)
		{
			out.writeByte(LIST);
			writeList(out, (List)f);
		} else
		if(f instanceof Map)
		{
			out.writeByte(MAP);
			writeMap(out, (Map)f);
		} else
		if(f instanceof BulkPushItem)
		{
			out.writeByte(BULKLISTITEM);
			BulkPushItem b = (BulkPushItem)f;
			b.writeExternal(out);
		} else if(f instanceof Byte)
		{
			out.writeByte(BYTE);
			out.writeByte(((Byte)f).byteValue());
		} else if(f instanceof Integer)
		{
			out.writeByte(INTEGER);
			out.writeInt(((Integer)f).intValue());
		} else if(f instanceof Long)
		{
			out.writeByte(LONG);
			out.writeLong(((Long)f).longValue());
		} else if(f instanceof Float)
		{
			out.writeByte(FLOAT);
			out.writeFloat(((Float)f).floatValue());
		} else if(f instanceof Double)
		{
			out.writeByte(DOUBLE);
			out.writeDouble(((Double)f).doubleValue());
		} else if(f instanceof Timestamp)
		{
			out.writeByte(TIMESTAMP);
			Timestamp ts = (Timestamp)f;
			out.writeLong(ts.getTime());
			out.writeInt(ts.getNanos());
		} else if(f instanceof Date)
		{
			out.writeByte(DATE);
			out.writeLong(((Date)f).getTime());
		} else
		{
			Byte id = class2IdMap.get(f.getClass());
			if(id != null)
			{
				out.writeByte(id);
				((Externalizable)f).writeExternal(out);
			}
			else
			{
				out.writeByte(0);
				out.writeObject(f);
			}
		}
	}
	
	/**
	 * This reads an object from the stream.
	 * @param in
	 * @return
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public Object readObject(ObjectInput in)
		throws IOException
	{
		try
		{
			byte b = in.readByte();
			if(b == 0)
			{
				return in.readObject();
			}
			else
			{
				switch(b)
				{
				case STRING:
					String s = in.readUTF();
					return s;
				case LIST:
					List list = readList(in);
					return list;
				case MAP:
					Map map = readMap(in);
					return map;
				case BYTE:
					return in.readByte();
				case INTEGER:
					return new Integer(in.readInt());
				case LONG:
					return new Long(in.readLong());
				case FLOAT:
					return new Float(in.readFloat());
				case DOUBLE:
					return new Double(in.readDouble());
				case TIMESTAMP:
					Timestamp ts = new Timestamp(in.readLong());
					ts.setNanos(in.readInt());
					return ts;
				case DATE:
					return new Date(in.readLong());
				case BULKLISTITEM:
					BulkPushItem bulk = new BulkPushItem();
					bulk.readExternal(in);
					return bulk;
				default:
					Class<? extends Externalizable> c = id2ClassMap.get(new Byte(b));
					Externalizable f = c.newInstance();
					f.readExternal(in);
					return f;
				}
			}
		}
		catch(ClassNotFoundException e)
		{
			logger.log(Level.SEVERE, "Unexpected exception inflating", e);
			throw new IOException(e);
		}
		catch(InstantiationException e)
		{
			logger.log(Level.SEVERE, "Unexpected exception inflating", e);
			throw new IOException(e);
		}
		catch(IllegalAccessException e)
		{
			logger.log(Level.SEVERE, "Unexpected exception inflating", e);
			throw new IOException(e);
		}
	}
	
	public <V> List<V> readList(ObjectInput in)
		throws IOException
	{
		int size = in.readInt();
		List<V> rc = new ArrayList<V>(size);
		for(int i = 0; i < size; ++i)
		{
			rc.add((V)readObject(in));
		}
		return rc;
	}
	
	/**
	 * This writes a list of non nullable values to
	 * a stream
	 * @param <V>
	 * @param out
	 * @param list
	 * @throws IOException
	 */
	public <V> void writeList(ObjectOutput out, List<V> list)
		throws IOException
	{
		int size = list.size();
		out.writeInt(size);
		for(int i = 0; i < size; ++i)
		{
			writeObject(out, list.get(i));
		}
	}
	
	public <K,V> Map<K,V> readMap(ObjectInput in)
		throws IOException
	{
		int size = in.readInt();
		Map<K,V> rc = new HashMap<K, V>();
		for(int i = 0; i < size; ++i)
		{
			K k = (K)readObject(in);
			V v = (V)readObject(in);
			rc.put(k, v);
		}
		return rc;
	}
	
	public <K,V> Map<K,V> readMapWithNullableValues(ObjectInput in)
		throws IOException
	{
		int size = in.readInt();
		Map<K,V> rc = new HashMap<K, V>();
		for(int i = 0; i < size; ++i)
		{
			K k = (K)readObject(in);
			V v = (V)readNullableObject(in);
			rc.put(k, v);
		}
		return rc;
	}

	/**
	 * This writes a Map of non null key and non null value pairs
	 * to the stream.
	 * @param <K>
	 * @param <V>
	 * @param out
	 * @param map
	 * @throws IOException
	 */
	public <K,V> void writeMap(ObjectOutput out, Map<K,V> map)
		throws IOException
	{
		int size = map.size();
		out.writeInt(size);
		for(Map.Entry<K,V> e : map.entrySet())
		{
			writeObject(out, e.getKey());
			writeObject(out, e.getValue());
		}
	}

	/**
	 * This writes a Map with non nullable keys and nullable Values
	 * to the stream.
	 * @param <K>
	 * @param <V>
	 * @param out
	 * @param map
	 * @throws IOException
	 */
	public <K,V> void writeMapWithNullableValues(ObjectOutput out, Map<K,V> map)
		throws IOException
	{
		int size = map.size();
		out.writeInt(size);
		for(Map.Entry<K,V> e : map.entrySet())
		{
			writeObject(out, e.getKey());
			writeNullableObject(out, e.getValue());
		}
	}
}
