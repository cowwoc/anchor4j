module io.github.cowwoc.anchor4j.container.buildx.test
{
	requires io.github.cowwoc.anchor4j.container.core.test;
	requires io.github.cowwoc.anchor4j.container.buildx;
	requires io.github.cowwoc.pouch.core;
	requires io.github.cowwoc.requirements12.java;
	requires org.slf4j;
	requires ch.qos.logback.core;
	requires ch.qos.logback.classic;
	requires org.apache.commons.compress;
	requires org.bouncycastle.pkix;
	requires org.bouncycastle.provider;
	requires com.fasterxml.jackson.databind;
	requires org.testng;
	requires io.github.cowwoc.anchor4j.container.core;

	opens io.github.cowwoc.anchor4j.container.buildx.test.resource to org.testng;
}