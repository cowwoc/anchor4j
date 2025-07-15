package io.github.cowwoc.anchor4j.core.internal.util;

import java.util.ArrayList;
import java.util.List;

public class Lists
{
	/**
	 * Combines one or more lists.
	 *
	 * @param lists the lists
	 * @return the combined list
	 */
	public static List<Object> combine(List<?>... lists)
	{
		int size = 0;
		for (List<?> list : lists)
			size += list.size();
		List<Object> combined = new ArrayList<>(size);
		for (List<?> list : lists)
			combined.addAll(list);
		return combined;
	}

	private Lists()
	{
	}
}