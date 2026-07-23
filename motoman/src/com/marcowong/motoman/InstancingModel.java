package com.marcowong.motoman;

import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class InstancingModel {
	private ModelData model;
	private Mesh[] meshes;
	private int[] nIndicesPerCopy;
	public int copies;
	
	public InstancingModel(ModelData model, int copies) {
		this.model = model;
		this.copies = copies;
		SubMeshData[] subMeshes = model.subMeshes;
		meshes = new Mesh[subMeshes.length];
		nIndicesPerCopy = new int[subMeshes.length];
		for (int i = 0; i < meshes.length; ++i) {
			MeshData meshData = subMeshes[i].mesh;
			
			java.util.ArrayList<VertexAttribute> attributes = new java.util.ArrayList<VertexAttribute>();
			attributes.add(new VertexAttribute(VertexAttributes.Usage.Position, 3, ShaderProgram.POSITION_ATTRIBUTE));
			if (meshData.hasNorms) attributes.add(new VertexAttribute(VertexAttributes.Usage.Normal, 3, ShaderProgram.NORMAL_ATTRIBUTE));
			if (meshData.hasUVs) attributes.add(new VertexAttribute(VertexAttributes.Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));
			if (meshData.hasSkeleton) attributes.add(new VertexAttribute(VertexAttributes.Usage.Generic, 1, "a_skeleton"));
			
			VertexAttribute[] vaa = attributes.toArray(new VertexAttribute[0]);
			
			int sizeVertex = 3 + (meshData.hasNorms ? 3 : 0) + (meshData.hasUVs ? 2 : 0) + (meshData.hasSkeleton ? 1 : 0);
			int nVertex = meshData.vertices.length / sizeVertex;
			float[] vertices = meshData.vertices;
			float[] vertices2 = new float[nVertex * sizeVertex * copies];
			int nIndices = meshData.indices != null ? meshData.indices.length : 0;
			short[] indices = meshData.indices != null ? meshData.indices : new short[0];
			short[] indices2 = new short [nIndices * copies];
			
			meshes[i] = new Mesh(true,
					nVertex * copies,
					nIndices * copies, vaa);
			
			nIndicesPerCopy[i] = nIndices;
			
			VertexAttribute skeAttr = null;
			int skeIdOffset = 0;
			for (int k = 0; k < vaa.length; ++k) {
				if (vaa[k].alias.equals("a_skeleton")) {
					skeAttr = vaa[k];
					skeIdOffset = vaa[k].offset / 4;
					break;
				}
			}
			
			int maxSkeId = 0;
			if (skeAttr != null) {
				for (int k = 0; k < nVertex; ++k) {
					int skeId = (int)Math.round(vertices[k * sizeVertex + skeIdOffset]);
					if (skeId > maxSkeId) maxSkeId = skeId;
				}
			}
			
			for (int k = 0; k < copies; ++k) {
				System.arraycopy(vertices, 0, vertices2, k * vertices.length, vertices.length);
				System.arraycopy(indices, 0, indices2, k * nIndices, indices.length);
				for (int l = 0; l < nIndices; ++l)
					indices2[k * nIndices + l] += k * nVertex;
				if (skeAttr != null) {
					for (int l = 0; l < nVertex; ++l) {
						int skeId = (int)Math.round(vertices2[k * vertices.length + l * sizeVertex + skeIdOffset]);
						if (skeId != 0)
							vertices2[k * vertices.length + l * sizeVertex + skeIdOffset] = skeId + k * maxSkeId;
					}
				}
			}
			
			meshes[i].setVertices(vertices2);
			if (indices2.length > 0) meshes[i].setIndices(indices2);
		}
	}
	
	public void render(ShaderProgram program, int nInst) {
		SubMeshData[] subMeshes = model.subMeshes;
		for (int i = 0; i < meshes.length; i++) {
			SubMeshData subMesh = subMeshes[i];
			if (i == 0) {
				subMesh.material.bind(program);
			} else if (!subMeshes[i - 1].material.equals(subMesh.material)) {
				subMesh.material.bind(program);
			}
			meshes[i].render(program, subMesh.primitiveType, 0, nInst * nIndicesPerCopy[i]);
		}
	}
	
	public void dispose() {
		for (int i = 0; i < meshes.length; ++i)
			meshes[i].dispose();
	}
}
