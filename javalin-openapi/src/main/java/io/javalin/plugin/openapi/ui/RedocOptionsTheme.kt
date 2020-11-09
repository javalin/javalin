package io.javalin.plugin.openapi.ui

data class RedocOptionsTheme(
    val spacingUnit: Int = 5,
    val sectionHorizontal: Int = 40,
    val sectionVertical: Int = 40,
    val breakpointsSmall: String = "50rem",
    val breakpointsMedium: String = "85rem",
    val breakpointsLarge: String = "105rem",
    val colorsTonalOffset: Double = 0.3,
    val typographyFontSize: String = "14px",
    val typographyLineHeight: String = "1.5em",
    val typographyFontWeightRegular: String = "400",
    val typographyFontWeightBold: String = "600",
    val typographyFontWeightLight: String = "300",
    val typographyFontFamily: String = "Roboto, sans-serif",
    val typographySmoothing: String = "antialiased",
    val isTypographyOptimizeSpeed: Boolean = true,
    val typographyHeadingsFontFamily: String = "Montserrat, sans-serif",
    val typographyHeadingsFontWeight: String = "400",
    val typographyHeadingsLineHeight: String = "1.6em",
    val typographyCodeFontSize: String = "13px",
    val typographyCodeFontFamily: String = "Courier, monospace",
    val typographyCodeColor: String = "#e53935",
    val typographyCodeBackgroundColor: String = "rgba(38, 50, 56, 0.05)",
    val isTypographyCodeWrap: Boolean = false,
    val menuWidth: String = "260px",
    val menuBackgroundColor: String = "#fafafa",
    val menuTextColor: String = "#333333",
    val menuGroupItemsTextTransform: String = "uppercase",
    val menuLevel1ItemsTextTransform: String = "none",
    val menuArrowSize: String = "1.5em",
    val logoGutter: String = "2px",
    val rightPanelBackgroundColor: String = "#263238",
    val rightPanelWidth: String = "40%",
    val rightPanelTextColor: String = "#ffffff"
) {
    
    class Builder {
        private var spacingUnit = 5
        private var sectionHorizontal = 40
        private var sectionVertical = 40

        private var breakpointsSmall = "50rem"
        private var breakpointsMedium = "85rem"
        private var breakpointsLarge = "105rem"

        private var colorsTonalOffset = 0.3

        private var typographyFontSize = "14px"
        private var typographyLineHeight = "1.5em"
        private var typographyFontWeightRegular = "400"
        private var typographyFontWeightBold = "600"
        private var typographyFontWeightLight = "300"
        private var typographyFontFamily = "Roboto, sans-serif"
        private var typographySmoothing = "antialiased"
        private var typographyOptimizeSpeed = true
        private var typographyHeadingsFontFamily = "Montserrat, sans-serif"
        private var typographyHeadingsFontWeight = "400"
        private var typographyHeadingsLineHeight = "1.6em"
        private var typographyCodeFontSize = "13px"
        private var typographyCodeFontFamily = "Courier, monospace"
        private var typographyCodeColor = "#e53935"
        private var typographyCodeBackgroundColor = "rgba(38, 50, 56, 0.05)"
        private var typographyCodeWrap = false

        private var menuWidth = "260px"
        private var menuBackgroundColor = "#fafafa"
        private var menuTextColor = "#333333"
        private var menuGroupItemsTextTransform = "uppercase"
        private var menuLevel1ItemsTextTransform = "none"
        private var menuArrowSize = "1.5em"

        private var logoGutter = "2px"

        private var rightPanelBackgroundColor = "#263238"
        private var rightPanelWidth = "40%"
        private var rightPanelTextColor = "#ffffff"

        fun setSpacingUnit(spacingUnit: Int): Builder {
            this.spacingUnit = spacingUnit
            return this
        }

        fun setSectionHorizontal(sectionHorizontal: Int): Builder {
            this.sectionHorizontal = sectionHorizontal
            return this
        }

        fun setSectionVertical(sectionVertical: Int): Builder {
            this.sectionVertical = sectionVertical
            return this
        }

        fun setBreakpointsSmall(breakpointsSmall: String): Builder {
            this.breakpointsSmall = breakpointsSmall
            return this
        }

        fun setBreakpointsMedium(breakpointsMedium: String): Builder {
            this.breakpointsMedium = breakpointsMedium
            return this
        }

        fun setBreakpointsLarge(breakpointsLarge: String): Builder {
            this.breakpointsLarge = breakpointsLarge
            return this
        }

        fun setColorsTonalOffset(colorsTonalOffset: Double): Builder {
            this.colorsTonalOffset = colorsTonalOffset
            return this
        }

        fun setTypographyFontSize(typographyFontSize: String): Builder {
            this.typographyFontSize = typographyFontSize
            return this
        }

        fun setTypographyLineHeight(typographyLineHeight: String): Builder {
            this.typographyLineHeight = typographyLineHeight
            return this
        }

        fun setTypographyFontWeightRegular(typographyFontWeightRegular: String): Builder {
            this.typographyFontWeightRegular = typographyFontWeightRegular
            return this
        }

        fun setTypographyFontWeightBold(typographyFontWeightBold: String): Builder {
            this.typographyFontWeightBold = typographyFontWeightBold
            return this
        }

        fun setTypographyFontWeightLight(typographyFontWeightLight: String): Builder {
            this.typographyFontWeightLight = typographyFontWeightLight
            return this
        }

        fun setTypographyFontFamily(typographyFontFamily: String): Builder {
            this.typographyFontFamily = typographyFontFamily
            return this
        }

        fun setTypographySmoothing(typographySmoothing: String): Builder {
            this.typographySmoothing = typographySmoothing
            return this
        }

        fun setTypographyOptimizeSpeed(typographyOptimizeSpeed: Boolean): Builder {
            this.typographyOptimizeSpeed = typographyOptimizeSpeed
            return this
        }

        fun setTypographyHeadingsFontFamily(typographyHeadingsFontFamily: String): Builder {
            this.typographyHeadingsFontFamily = typographyHeadingsFontFamily
            return this
        }

        fun setTypographyHeadingsFontWeight(typographyHeadingsFontWeight: String): Builder {
            this.typographyHeadingsFontWeight = typographyHeadingsFontWeight
            return this
        }

        fun setTypographyHeadingsLineHeight(typographyHeadingsLineHeight: String): Builder {
            this.typographyHeadingsLineHeight = typographyHeadingsLineHeight
            return this
        }

        fun setTypographyCodeFontSize(typographyCodeFontSize: String): Builder {
            this.typographyCodeFontSize = typographyCodeFontSize
            return this
        }

        fun setTypographyCodeFontFamily(typographyCodeFontFamily: String): Builder {
            this.typographyCodeFontFamily = typographyCodeFontFamily
            return this
        }

        fun setTypographyCodeColor(typographyCodeColor: String): Builder {
            this.typographyCodeColor = typographyCodeColor
            return this
        }

        fun setTypographyCodeBackgroundColor(typographyCodeBackgroundColor: String): Builder {
            this.typographyCodeBackgroundColor = typographyCodeBackgroundColor
            return this
        }

        fun setTypographyCodeWrap(typographyCodeWrap: Boolean): Builder {
            this.typographyCodeWrap = typographyCodeWrap
            return this
        }

        fun setMenuWidth(menuWidth: String): Builder {
            this.menuWidth = menuWidth
            return this
        }

        fun setMenuBackgroundColor(menuBackgroundColor: String): Builder {
            this.menuBackgroundColor = menuBackgroundColor
            return this
        }

        fun setMenuTextColor(menuTextColor: String): Builder {
            this.menuTextColor = menuTextColor
            return this
        }

        fun setMenuGroupItemsTextTransform(menuGroupItemsTextTransform: String): Builder {
            this.menuGroupItemsTextTransform = menuGroupItemsTextTransform
            return this
        }

        fun setMenuLevel1ItemsTextTransform(menuLevel1ItemsTextTransform: String): Builder {
            this.menuLevel1ItemsTextTransform = menuLevel1ItemsTextTransform
            return this
        }

        fun setMenuArrowSize(menuArrowSize: String): Builder {
            this.menuArrowSize = menuArrowSize
            return this
        }

        fun setLogoGutter(logoGutter: String): Builder {
            this.logoGutter = logoGutter
            return this
        }

        fun setRightPanelBackgroundColor(rightPanelBackgroundColor: String): Builder {
            this.rightPanelBackgroundColor = rightPanelBackgroundColor
            return this
        }

        fun setRightPanelWidth(rightPanelWidth: String): Builder {
            this.rightPanelWidth = rightPanelWidth
            return this
        }

        fun setRightPanelTextColor(rightPanelTextColor: String): Builder {
            this.rightPanelTextColor = rightPanelTextColor
            return this
        }

        fun build(): RedocOptionsTheme {
            return RedocOptionsTheme(spacingUnit, sectionHorizontal, sectionVertical, breakpointsSmall, breakpointsMedium, breakpointsLarge, colorsTonalOffset, typographyFontSize, typographyLineHeight, typographyFontWeightRegular, typographyFontWeightBold, typographyFontWeightLight, typographyFontFamily, typographySmoothing, typographyOptimizeSpeed, typographyHeadingsFontFamily, typographyHeadingsFontWeight, typographyHeadingsLineHeight, typographyCodeFontSize, typographyCodeFontFamily, typographyCodeColor, typographyCodeBackgroundColor, typographyCodeWrap, menuWidth, menuBackgroundColor, menuTextColor, menuGroupItemsTextTransform, menuLevel1ItemsTextTransform, menuArrowSize, logoGutter, rightPanelBackgroundColor, rightPanelWidth, rightPanelTextColor)
        }
    }

}
