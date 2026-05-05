package com.tomdh.courseapi.scalar

import com.netflix.graphql.dgs.DgsComponent
import com.netflix.graphql.dgs.DgsRuntimeWiring
import graphql.scalars.ExtendedScalars
import graphql.schema.idl.RuntimeWiring.Builder

@DgsComponent
class JsonScalarRegistration {
    @DgsRuntimeWiring
    fun addScalar(builder: Builder): Builder {
        return builder.scalar(ExtendedScalars.Json)
    }
}