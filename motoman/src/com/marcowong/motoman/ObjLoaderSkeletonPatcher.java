package com.marcowong.motoman;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes;

public class ObjLoaderSkeletonPatcher {
	public final static Color[] skeletonIdColors = new Color[] {
		Color.WHITE, Color.BLACK, Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.CYAN,
		Color.BLUE, Color.MAGENTA, Color.PINK, Color.LIGHT_GRAY, Color.GRAY, Color.DARK_GRAY
	};
	
	public void patch(ModelData model, Pixmap skeletonMapping) {
		for (SubMeshData subMesh : model.subMeshes) {
			MeshData mesh = subMesh.mesh;
			int sizeVertex = 3 + (mesh.hasNorms ? 3 : 0) + (mesh.hasUVs ? 2 : 0);
			int nVertex = mesh.vertices.length / sizeVertex;
			float[] vertices = mesh.vertices;
			float[] vertices2 = new float[nVertex * (sizeVertex + 1)];
			
			int uvOffset = 3 + (mesh.hasNorms ? 3 : 0);
			
			for (int i = 0; i < nVertex; ++i) {
				int offset = i * sizeVertex;
				int offset2 = i * (sizeVertex + 1);
				int offsetUV = offset2 + uvOffset;
				int offsetSke = offset2 + sizeVertex;
				for (int j = 0; j < sizeVertex; ++j) vertices2[offset2 + j] = vertices[offset + j];
				float u = 0;
				float v = 0;
				if (mesh.hasUVs) {
					u = vertices2[offsetUV];
					v = vertices2[offsetUV + 1];
				}
				vertices2[offsetSke] = getSkeletonId(skeletonMapping, u, v);
			}
			
			mesh.vertices = vertices2;
			mesh.hasSkeleton = true;
		}
	}
	
	private Color tmpColor = new Color();
	private int getSkeletonId(Pixmap m, float u, float v) {
		int x = (int)Math.round(u * m.getWidth());
		int y = (int)Math.round(v * m.getHeight());
		Color.rgba8888ToColor(tmpColor, m.getPixel(x, y));
		int id = 0;
		float lowestDiff = Float.POSITIVE_INFINITY;
		for (int i = 0; i < skeletonIdColors.length; ++i) {
			float diff = 
				Math.abs(skeletonIdColors[i].r - tmpColor.r) +
				Math.abs(skeletonIdColors[i].g - tmpColor.g) +
				Math.abs(skeletonIdColors[i].b - tmpColor.b);
			if (diff < lowestDiff) {
				id = i;
				lowestDiff = diff;
			}
		}
		return id;
	}
}
