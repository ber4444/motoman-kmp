package com.marcowong.motoman;

import java.util.ArrayList;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.Texture.TextureFilter;


public class StaticModelTextureFilterConfigManager {
	private static ArrayList<Texture> textures = new ArrayList<Texture>();
	
	public static void add(Texture t) {
		textures.add(t);
	}
	
	public static void remove(Texture t) {
		textures.remove(t);
	}
	
	public static void add(ModelData m) {
		SubMeshData[] meshes = m.subMeshes;
		for (SubMeshData mesh : meshes) {
			if (mesh.material != null && mesh.material.diffuseTexture != null) {
				add(mesh.material.diffuseTexture);
			}
		}
	}
	
	public static void remove(ModelData m) {
		SubMeshData[] meshes = m.subMeshes;
		for (SubMeshData mesh : meshes) {
			if (mesh.material != null && mesh.material.diffuseTexture != null) {
				remove(mesh.material.diffuseTexture);
			}
		}
	}
	
	public static void updateFilter() {
		for (Texture t : textures) {
			if (ConfigHelper.turnOnModelTextureLinearFilter())
				t.setFilter(TextureFilter.Linear, TextureFilter.Linear);
			else
				t.setFilter(TextureFilter.Nearest, TextureFilter.Nearest);
		}
	}
}
