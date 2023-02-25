package com.skdziwak.mandelbrot

import scala.annotation.targetName

case class ComplexNumber(real: Double, imaginary: Double) {
  @targetName("addition")
  def +(other: ComplexNumber): ComplexNumber = ComplexNumber(real + other.real, imaginary + other.imaginary)

  @targetName("multiplication")
  def *(other: ComplexNumber): ComplexNumber = ComplexNumber(real * other.real - imaginary * other.imaginary, real * other.imaginary + imaginary * other.real)

  @targetName("division")
  def /(other: ComplexNumber): ComplexNumber = {
    val denominator = other.real * other.real + other.imaginary * other.imaginary
    ComplexNumber((real * other.real + imaginary * other.imaginary) / denominator, (imaginary * other.real - real * other.imaginary) / denominator)
  }

  @targetName("subtraction")
  def -(other: ComplexNumber): ComplexNumber = ComplexNumber(real - other.real, imaginary - other.imaginary)
  
  def abs: Double = Math.sqrt(real * real + imaginary * imaginary)
}