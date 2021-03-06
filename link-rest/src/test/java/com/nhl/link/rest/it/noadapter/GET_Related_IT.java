package com.nhl.link.rest.it.noadapter;

import static org.junit.Assert.assertEquals;

import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.junit.Test;

import com.nhl.link.rest.it.fixture.JerseyTestOnDerby;
import com.nhl.link.rest.it.fixture.cayenne.E18;
import com.nhl.link.rest.it.fixture.resource.E12Resource;
import com.nhl.link.rest.it.fixture.resource.E17Resource;
import com.nhl.link.rest.it.fixture.resource.E18Resource;
import com.nhl.link.rest.it.fixture.resource.E2Resource;
import com.nhl.link.rest.it.fixture.resource.E3Resource;

public class GET_Related_IT extends JerseyTestOnDerby {

	@Override
	protected void doAddResources(FeatureContext context) {
		context.register(E2Resource.class);
		context.register(E3Resource.class);
		context.register(E12Resource.class);
		context.register(E17Resource.class);
		context.register(E18Resource.class);
	}

	@Test
	public void testGet_ToMany_Constrained() {

		// make sure we have e3s for more than one e2 - this will help us
		// confirm that relationship queries are properly filtered.

		insert("e2", "id, name", "1, 'xxx'");
		insert("e2", "id, name", "2, 'yyy'");

		insert("e3", "id, e2_id, name", "7, 2, 'zzz'");
		insert("e3", "id, e2_id, name", "8, 1, 'yyy'");
		insert("e3", "id, e2_id, name", "9, 1, 'zzz'");

		Response r1 = target("/e2/constraints/1/e3s").request().get();

		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals("{\"data\":[{\"id\":8},{\"id\":9}],\"total\":2}", r1.readEntity(String.class));
	}

	@Test
	public void testGet_ToMany_CompoundId() {

		insert("e17", "id1, id2, name", "1, 1, 'aaa'");
		insert("e17", "id1, id2, name", "2, 2, 'bbb'");
		insert("e18", "id, e17_id1, e17_id2, name", "1, 1, 1, 'xxx'");
		insert("e18", "id, e17_id1, e17_id2, name", "2, 1, 1, 'yyy'");
		insert("e18", "id, e17_id1, e17_id2, name", "3, 2, 2, 'zzz'");

		Response r1 = target("/e17/e18s").matrixParam("parentId1", 1).matrixParam("parentId2", 1).request().get();

		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals("{\"data\":[{\"id\":1,\"name\":\"xxx\"},{\"id\":2,\"name\":\"yyy\"}],\"total\":2}",
				r1.readEntity(String.class));
	}

	@Test
	public void testGet_ValidRel_ToOne_CompoundId() {

		insert("e17", "id1, id2, name", "1, 1, 'aaa'");
		insert("e17", "id1, id2, name", "2, 2, 'bbb'");
		insert("e18", "id, e17_id1, e17_id2, name", "1, 1, 1, 'xxx'");
		insert("e18", "id, e17_id1, e17_id2, name", "2, 1, 1, 'yyy'");
		insert("e18", "id, e17_id1, e17_id2, name", "3, 2, 2, 'zzz'");

		Response r1 = target("/e18/1").queryParam("include", E18.E17.getName()).request().get();

		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals(
				"{\"data\":[{\"id\":1," + "\"e17\":{\"id\":{\"id1\":1,\"id2\":1},\"id1\":1,\"id2\":1,\"name\":\"aaa\"},"
						+ "\"name\":\"xxx\"}],\"total\":1}",
				r1.readEntity(String.class));
	}

	@Test
	public void testGet_CompoundId_UnmappedPk() {

		// remove a part of PK from the ObjEntity
		DataMap dataMap = DB_STACK.getCayenneStack().getChannel().getEntityResolver().getDataMap("datamap");
		ObjEntity E17 = dataMap.getObjEntity("E17");
		ObjAttribute unmappedAttribute = E17.getAttribute("id2");
		E17.removeAttribute("id2");

		insert("e17", "id1, id2, name", "1, 1, 'aaa'");
		insert("e17", "id1, id2, name", "2, 2, 'bbb'");
		insert("e18", "id, e17_id1, e17_id2, name", "1, 1, 1, 'xxx'");
		insert("e18", "id, e17_id1, e17_id2, name", "2, 1, 1, 'yyy'");
		insert("e18", "id, e17_id1, e17_id2, name", "3, 2, 2, 'zzz'");

		Response r1 = target("/e18/1").queryParam("include", E18.E17.getName()).request().get();

		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals("{\"data\":[{\"id\":1,\"" + "e17\":{\"id\":{\"id1\":1,\"id2\":1},\"id1\":1,\"name\":\"aaa\"},"
				+ "\"name\":\"xxx\"}],\"total\":1}", r1.readEntity(String.class));

		// restore initial state
		E17.addAttribute(unmappedAttribute);
	}

	@Test
	public void testGet_ValidRel_ToMany() {

		// make sure we have e3s for more than one e2 - this will help us
		// confirm that relationship queries are properly filtered.

		insert("e2", "id, name", "1, 'xxx'");
		insert("e2", "id, name", "2, 'yyy'");
		insert("e3", "id, e2_id, name", "7, 2, 'zzz'");
		insert("e3", "id, e2_id, name", "8, 1, 'yyy'");
		insert("e3", "id, e2_id, name", "9, 1, 'zzz'");

		Response r1 = target("/e2/1/e3s").queryParam("include", "id").request().get();

		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals("{\"data\":[{\"id\":8},{\"id\":9}],\"total\":2}", r1.readEntity(String.class));
	}

	@Test
	public void testGet_ValidRel_ToOne() {

		// make sure we have e3s for more than one e2 - this will help us
		// confirm that relationship queries are properly filtered.

		insert("e2", "id, name", "1, 'xxx'");
		insert("e2", "id, name", "2, 'yyy'");
		insert("e3", "id, e2_id, name", "7, 2, 'zzz'");
		insert("e3", "id, e2_id, name", "8, 1, 'yyy'");
		insert("e3", "id, e2_id, name", "9, 1, 'zzz'");

		Response r1 = target("/e3/7/e2").queryParam("include", "id").request().get();

		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals("{\"data\":[{\"id\":2}],\"total\":1}", r1.readEntity(String.class));
	}

	@Test
	public void testGet_InvalidRel() {
		Response r1 = target("/e2/1/dummyrel").request().get();

		assertEquals(Status.BAD_REQUEST.getStatusCode(), r1.getStatus());
		assertEquals("{\"success\":false,\"message\":\"Invalid relationship: 'dummyrel'\"}",
				r1.readEntity(String.class));
	}

	@Test
	public void testGET_ToManyJoin() {

		insert("e12", "id", "11");
		insert("e12", "id", "12");
		insert("e13", "id", "14");
		insert("e13", "id", "15");
		insert("e13", "id", "16");

		insert("e12_e13", "e12_id, e13_id", "11, 14");
		insert("e12_e13", "e12_id, e13_id", "12, 16");

		// excluding ID - can't render multi-column IDs yet
		Response r1 = target("/e12/12/e1213").queryParam("exclude", "id").queryParam("include", "e12")
				.queryParam("include", "e13").request().get();

		assertEquals(Status.OK.getStatusCode(), r1.getStatus());
		assertEquals("{\"data\":[{\"e12\":{\"id\":12},\"e13\":{\"id\":16}}],\"total\":1}", r1.readEntity(String.class));
	}
}
