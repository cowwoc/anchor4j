package io.github.cowwoc.anchor4j.core.internal.util;

/**
 * Thread helper functions.
 */
public final class Threads
{
	/**
	 * Returns a thread's name. If the name is empty, returns "virtual-<threadId>" for virtual threads or
	 * "platform-<threadId>" for platform threads.
	 *
	 * @param thread the thread
	 * @return the name
	 * @throws NullPointerException if {@code thread} is null
	 */
	public static String getName(Thread thread)
	{
		String name = thread.getName();
		if (!name.isEmpty())
			return name;
		if (thread.isVirtual())
			return "virtual-" + thread.threadId();
		return "platform-" + thread.threadId();
	}

	private Threads()
	{
	}
}
