/* Copyright 2019, T.AI Labs. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.platanios.tensorflow.api.ops

import org.platanios.tensorflow.api.core.Shape
import org.platanios.tensorflow.api.core.exception.InvalidArgumentException
import org.platanios.tensorflow.api.core.types._
import org.platanios.tensorflow.api.implicits.Implicits._
import org.platanios.tensorflow.api.tensors
import org.platanios.tensorflow.api.tensors.Tensor
import org.platanios.tensorflow.api.utilities.DefaultsTo.IntDefault
//import com.google.protobuf.ByteString.Output

import org.tensorflow.framework.AttrValue

import scala.language.postfixOps
import com.google.protobuf.Descriptors.FieldDescriptor

/**
  * Defines linear algebra ops similar to the
  * ones defined in tf.linalg package of the Python TF API
  *
  *
  */
trait Linalg {

  /**
    * Performs cholesky decomposition of one or more self-adjoint matrices.
    *
    * The input is a tensor of shape [..., M, M] whose inner-most 2 
    * dimensions form square matrices.
    *
    * The input has to be symmetric and positive definite. Only the lower-triangular 
    * part of the input will be used for this operation. The upper-triangular part 
    * will not be read. The output is a tensor of the same shape as the input 
    * containing the Cholesky decompositions for all input submatrices `[..., :, :]`. 
    * **Note**: The gradient computation on GPU is faster for large matrices but 
    * not for large batch dimensions when the submatrices are small. In this 
    * case it might be faster to use the CPU.
    * 
    * Returns: 
    * Output of shape [M, M]
    *
    * @tparam T The underlying scala type of the matrix elements.
    * @param matrix The input.
    * 
    * @param name An optional name to assign to the op.
    *
    */
  def cholesky[T: TF: IsRealOrComplex](matrix: Output[T], name: String = "Cholesky"): Output[T] =
    Op.Builder[Output[T], Output[T]](
        opType = "Cholesky",
        name = name,
        input = matrix
      ).setGradientFn(choleskyGrad(_, _)(TF[T], IsRealOrComplex[T])).build().output

  protected def choleskyGrad[T: TF: IsRealOrComplex](
      l: Op[Output[T], Output[T]],
      outputGradient: Output[T]
  ): Output[T] =
    Op.Builder[(Output[T], Output[T]), Output[T]](
        opType = "CholeskyGrad",
        name = "CholeskyGrad",
        input = (l.output, outputGradient)
      ).build().output

  def matrixDeterminant[T: TF: IsRealOrComplex](matrix: Output[T], name: String = "MatrixDeterminant"): Output[T] = {
    Op.Builder[Output[T], Output[T]](
        opType = "MatrixDeterminant",
        name = name,
        input = matrix
      ).build().output
  }

  /**
    * Computes (sign(det(x)) log(|det(x)|)) for an input x.
    *
    * @tparam T The underlying scala type of the matrix elements.
    *
    * @param matrix A matrix of shape [N, M, M]
    * @param name An optional name to assign to the op.
    *
    * @return A tuple having the results.
    *
    */
  def logMatrixDeterminant[T: TF: IsRealOrComplex](
      matrix: Output[T],
      name: String = "LogMatrixDeterminant"
  ): (Output[T], Output[T]) = {
    Op.Builder[Output[T], (Output[T], Output[T])](
        opType = "LogMatrixDeterminant",
        name = name,
        input = matrix
      ).build().output
  }

  /**
    * Computes inv(A), assuming matrix A is invertible and of shape [..., M, M]
    *
    * @tparam T The underlying scala type of the matrix elements.
    * @param matrix The matrix to invert.
    * @param adjoint If set to true, returns the adjoint, defaults to false.
    * @param name An optional name to assign to the op.
    *
    */
  def matrixInverse[T: TF: IsRealOrComplex](
      matrix: Output[T],
      adjoint: Boolean = false,
      name: String = "MatrixInverse"
  ): Output[T] =
    Op.Builder[Output[T], Output[T]](
        opType = "MatrixInverse",
        name = name,
        input = matrix
      ).setAttribute("adjoint", adjoint).build().output

  /**
    * Solves systems of linear equations Ax = b.
    * The matrix M must be of shape [..., M, M] whose inner-most 2 dimensions
    * form square matrices.
    *
    * The right hand side b is a tensor of shape [..., M, K].
    * The output x is a tensor shape [..., M, K]
    *
    * If `adjoint` is `True` then each output matrix satisfies
    * adjoint(A[..., :, :]) * x[..., :, :] = b[..., :, :].
    *
    * If `adjoint` is `False` then each output matrix satisfies
    * A[..., :, :] * x[..., :, :] = b[..., :, :].
    *
    * @tparam T The underlying scala type of the matrix elements.
    * @param matrix The matrix (A) on the left hand side.
    * @param rhs The right hand side (b).
    * @param adjoint Defaults to false.
    * @param name An optional name to assign to the op.
    *
    */
  def matrixSolve[T: TF: IsRealOrComplex](
      matrix: Output[T],
      rhs: Output[T],
      adjoint: Boolean = false,
      name: String = "MatrixSolve"
  ): Output[T] =
    Op.Builder[(Output[T], Output[T]), Output[T]](
        opType = "MatrixSolve",
        name = name,
        input = (matrix, rhs)
      ).setAttribute("adjoint", adjoint).build().output

  /**
    * Solves systems of linear equations Ax = b, in the regularised
    * least squares sense.
    *
    * The matrix A must be of shape [..., M, N] whose inner-most 2 dimensions
    * form square matrices.
    *
    * The right hand side b is a tensor of shape [..., M, K].
    * The output x is a tensor shape [..., N, K]
    *
    *
    * @tparam T The underlying scala type of the matrix elements.
    * @param matrix The matrix (A) on the left hand side.
    * @param rhs The right hand side (b).
    * @param reg The L2 regularisation constant.
    * @param fast Defaults to true.
    * @param name An optional name to assign to the op.
    *
    */
  def matrixSolveLS[T: TF: IsRealOrComplex](
      matrix: Output[T],
      rhs: Output[T],
      reg: Output[T],
      fast: Boolean = true,
      name: String = "MatrixSolveLs"
  ): Output[T] =
    Op.Builder[(Output[T], Output[T], Output[T]), Output[T]](
        opType = "MatrixSolveLs",
        name = name,
        input = (matrix, rhs, reg)
      ).setAttribute("fast", fast).build().output

  /* def matrixSquareRoot[T: TF: IsRealOrComplex](matrix: Output[T], name: String = "MatrixSquareRoot"): Output[T] = {
    Op.Builder[Output[T], Output[T]](
        opType = "MatrixSquareRoot",
        name = name,
        input = matrix
      ).build().output
  } */

  def matrixTriangularSolve[T: TF: IsRealOrComplex](
      matrix: Output[T],
      rhs: Output[T],
      lower: Boolean = true,
      adjoint: Boolean = false,
      name: String = "MatrixTriangularSolve"
  ): Output[T] =
    Op.Builder[(Output[T], Output[T]), Output[T]](
        opType = "MatrixTriangularSolve",
        name = name,
        input = (matrix, rhs)
      ).setAttribute("lower", lower).setAttribute("adjoint", adjoint).build().output

  /**
    * Performs QR decomposition of a matrix.
    *
    * The matrix must be of [..., M, N] whose inner-most 2 dimensions
    * form matrices of size [M, N]. Let P be the minimum of M and N.
    *
    * Returns:
    * q: Orthonormal basis for range of the input matrix. If
    * full_matrices is `False` then shape is [..., M, P];
    * if full_matrices is `True` then shape is [..., M, M].
    * r: Triangular factor. If full_matrices is `False` then shape is
    * [..., P, N]. If full_matrices is `True` then shape is [..., M, N].
    *
    *
    * @tparam T The underlying scala type of the matrix elements.
    * @param matrix The input.
    * @param full_matrices If true, compute full-sized q and r.
    *                      If false (the default), compute only the
    *                      leading P columns of q.
    * @param name An optional name to assign to the op.
    *
    */
  def qr[T: TF: IsRealOrComplex](
      matrix: Output[T],
      full_matrices: Boolean = false,
      name: String = "Qr"
  ): (Output[T], Output[T]) =
    Op.Builder[Output[T], (Output[T], Output[T])](
        opType = "Qr",
        name = name,
        input = matrix
      ).setAttribute("full_matrices", full_matrices).build().output

  def selfAdjointEig[T: TF: IsRealOrComplex](
      matrix: Output[T],
      compute_v: Boolean = true,
      name: String = "SelfAdjointEigV2"
  ): (Output[T], Output[T]) =
    Op.Builder[Output[T], (Output[T], Output[T])](
        opType = "SelfAdjointEigV2",
        name = name,
        input = matrix
      ).setAttribute("compute_v", compute_v).build().output

  /**
    * Performs singular value decomposition of a matrix.
    *
    * The matrix must be of [..., M, N] whose inner-most 2 dimensions
    * form matrices of size [M, N]. Let P be the minimum of M and N.
    *
    * Returns: 
    * s: Singular values. Shape is [..., P]. 
    * u: Left singular vectors. If full_matrices is False then shape is 
    * [..., M, P]; if full_matrices is True then shape is 
    * [..., M, M]. Undefined if compute_uv is False. 
    * v: Left singular vectors. If full_matrices is False then shape is 
    * [..., N, P]. If full_matrices is True then shape is [..., N, N]. 
    * Undefined if compute_uv is false.
    *
    *
    * @tparam T The underlying scala type of the matrix elements.
    * @param matrix The matrix to decompose.
    * @param full_matrices If true, compute full-sized u and v.
    *                      If false (the default), compute only the
    *                      leading P singular vectors.
    * @param compute_uv If true, left and right singular vectors will be 
    *                   computed and returned in u and v, respectively.
    * @param name An optional name to assign to the op.
    *
    */
  def svd[T: TF: IsRealOrComplex](
      matrix: Output[T],
      compute_uv: Boolean = true,
      full_matrices: Boolean = false,
      name: String = "Svd"
  ): (Output[T], Output[T], Output[T]) =
    Op.Builder[Output[T], (Output[T], Output[T], Output[T])](
        opType = "Svd",
        name = name,
        input = matrix
      ).setAttribute("compute_uv", compute_uv).setAttribute("full_matrices", full_matrices).build().output
}
