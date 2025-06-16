module io.github.cowwoc.anchor4j.docker.test
{
	requires io.github.cowwoc.anchor4j.core.test;
	requires io.github.cowwoc.anchor4j.docker;
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.java;
	requires org.slf4j;
	requires ch.qos.logback.core;
	requires ch.qos.logback.classic;
	requires org.apache.commons.compress;
	requires org.bouncycastle.pkix;
	requires org.bouncycastle.provider;
	requires org.testng;
	requires com.fasterxml.jackson.annotation;

	opens io.github.cowwoc.anchor4j.docker.test.resource to org.testng;
	opens io.github.cowwoc.anchor4j.docker.test to org.testng;
}