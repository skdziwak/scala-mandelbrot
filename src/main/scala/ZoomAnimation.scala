package com.skdziwak.mandelbrot

object ZoomAnimation {
    def animate(startZoom: Double, template: MandelbrotTemplate, exporter: MandelbrotExport, multiplier: Double, progressListener: ProgressListener): AnimationSummary = {
        val targetZoom = template.zoom
        var currentZoom = startZoom
        val totalFrames = calculateFrames(startZoom, template, multiplier)
        var frame = 0
        progressListener.progress(frame, totalFrames)
        while (currentZoom < targetZoom) {
            currentZoom = currentZoom * multiplier
            val zoomedTemplate = template.copy(zoom = currentZoom)
            exporter.addFrame(zoomedTemplate)

            frame += 1
            progressListener.progress(frame, totalFrames)
        }
        AnimationSummary(totalFrames)
    }

    def calculateFrames(startZoom: Double, template: MandelbrotTemplate, multiplier: Double): Int = {
        val targetZoom = template.zoom
        Math.ceil(Math.log(targetZoom / startZoom) / Math.log(multiplier)).toInt
    }

}

object ZoomAnimationWithSlowEnd {
    def animate(startZoom: Double, template: MandelbrotTemplate, exporter: MandelbrotExport, multiplier: Double, progressListener: ProgressListener, endFrames: Int): AnimationSummary = {
        if (endFrames == 0) {
            return ZoomAnimation.animate(startZoom, template, exporter, multiplier, progressListener)
        }
        val zoomInFrames = ZoomAnimation.calculateFrames(startZoom, template, multiplier)
        val totalFrames = zoomInFrames + endFrames

        val summary = ZoomAnimation.animate(startZoom, template, exporter, multiplier, (a, _) => {
            progressListener.progress(a, totalFrames)
        })
        var acceleration = multiplier
        val targetAcceleration = 1 - (0.1 - multiplier / 10)
        val accelerationMultiplier = Math.pow(targetAcceleration / multiplier, 1.0 / endFrames)
        for (i <- 1 to endFrames) {
            acceleration = acceleration * accelerationMultiplier
            val zoomedTemplate = template.copy(zoom = template.zoom * acceleration)
            exporter.addFrame(zoomedTemplate)
            progressListener.progress(zoomInFrames + i, totalFrames)
        }
        summary
    }

    def calculateFrames(startZoom: Double, template: MandelbrotTemplate, multiplier: Double, endFrames: Int): Int = {
        ZoomAnimation.calculateFrames(startZoom, template, multiplier) + endFrames
    }
}

case class AnimationSummary(totalFrames: Int)

trait ProgressListener {
    def progress(current: Int, total: Int): Unit
}