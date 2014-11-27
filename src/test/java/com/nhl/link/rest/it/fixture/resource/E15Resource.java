package com.nhl.link.rest.it.fixture.resource;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import com.nhl.link.rest.DataResponse;
import com.nhl.link.rest.it.fixture.cayenne.E14;
import com.nhl.link.rest.it.fixture.cayenne.E15;

@Path("e15")
public class E15Resource extends LrResource {

	@PUT
	@Path("{id}/e14s")
	// note that parent id is "int" here , but is BIGINT (long) in the DB. This
	// is intentional
	public DataResponse<E14> relateToOneExisting(@PathParam("id") int id, String data) {
		return getService().idempotentFullSync(E14.class).toManyParent(E15.class, id, E15.E14S).includeData()
				.process(data);
	}
}
