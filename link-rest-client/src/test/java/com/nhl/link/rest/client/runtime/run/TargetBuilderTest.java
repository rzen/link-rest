package com.nhl.link.rest.client.runtime.run;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.FeatureContext;

import org.junit.Test;

import com.nhl.link.rest.client.protocol.Include;
import com.nhl.link.rest.client.protocol.LrcRequest;
import com.nhl.link.rest.client.protocol.Sort;
import com.nhl.link.rest.it.fixture.JerseyTestOnDerby;

public class TargetBuilderTest extends JerseyTestOnDerby {

	@Test
	public void testBuild_Target_NoConstraints() {

		WebTarget target = target("/path/to/resource");

		WebTarget newTarget = TargetBuilder.target(target).request(LrcRequest.builder().build()).build();

		assertEquals(target.getUri(), newTarget.getUri());
	}

	@Test
	public void testBuild_Target_Constrained1() throws Exception {

		LrcRequest request = LrcRequest.builder().exclude("ex1", "ex2").exclude("ex3").include("in1")
				.include(Include.path("in2"))
				.include(Include.path("in3").sort(Sort.property("in3.s1").desc()).limit(100)).build();

		WebTarget target = target("/path/to/resource");
		WebTarget newTarget = TargetBuilder.target(target).request(request).build();

		String query = newTarget.getUri().getQuery();
		assertEquals(
				"exclude=ex3&exclude=ex2&exclude=ex1&include=in2&include=in1&"
						+ "include={\"path\":\"in3\",\"limit\":100,\"sort\":[{\"property\":\"in3.s1\",\"direction\":\"DESC\"}]}",
				query);
	}

	@Override
	protected void doAddResources(FeatureContext context) {
		// do nothing...
	}
}