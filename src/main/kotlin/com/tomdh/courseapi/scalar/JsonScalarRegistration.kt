package com.tomdh.courseapi.scalar

import graphql.schema.idl.RuntimeWiring.Builder

@com.netflix.graphql.dgs.DgsComponent
class JsonScalarRegistration {
    @com.netflix.graphql.dgs.DgsRuntimeWiring
    fun addScalar(builder: Builder): Builder {
        return builder.scalar(graphql.scalars.ExtendedScalars.Json)
    }
}