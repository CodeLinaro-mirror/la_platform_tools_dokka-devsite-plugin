package com.google.devsite.components.impl

import com.google.devsite.components.ShouldBreak
import com.google.devsite.components.render
import com.google.devsite.components.symbols.MappedTypeProjectionComponent
import kotlinx.html.FlowContent

internal class DefaultMappedTypeProjectionComponent(
    override val data: MappedTypeProjectionComponent.Params
) : MappedTypeProjectionComponent {
    override fun render(into: FlowContent) = into.run {
        data.annotationComponents.render(into, separator = "", terminator = { +" " })
        +"("
        data.alternativePrefix.render(into)
        +")"
        data.type.render(into)
        data.generics.render(into, ShouldBreak.NO, brackets = "<>")
        +data.nullability.renderAsKotlinSuffix()
    }
}
