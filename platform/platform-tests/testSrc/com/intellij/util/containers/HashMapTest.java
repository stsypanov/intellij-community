// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.containers;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;

@RunWith(value = Parameterized.class)
public class HashMapTest {
  private String mapType;

  public HashMapTest(String mapType) {
    this.mapType = mapType;
  }

  @Parameterized.Parameters
  public static Collection<String> data() {
    return Arrays.asList("jdk", "idea");
  }

  @Test
  public void containsKey() {
    Map<Integer, Integer> map = freshMap();

    assertFalse(map.containsKey(1));
    assertFalse(map.containsKey(null));

    map.put(1, 2);
    assertTrue(map.containsKey(1));

    map.clear();
    assertFalse(map.containsKey(1));

    map.put(null, 2);
    assertTrue(map.containsKey(null));

    map.clear();
    assertFalse(map.containsKey(null));
  }

  @Test
  public void containsValue() {
    Map<Integer, Integer> map = freshMap();

    assertFalse(map.containsValue(1));
    assertFalse(map.containsValue(null));

    map.put(2, 1);
    assertTrue(map.containsValue(1));

    map.clear();
    assertFalse(map.containsValue(1));

    map.put(1, null);
    assertTrue(map.containsValue(null));

    map.clear();
    assertFalse(map.containsValue(null));
  }

  @Test
  public void get() {
    Map<Integer, Integer> map = freshMap();
    assertThat(map.get(1)).isNull();

    map.put(1, 2);
    assertThat(map.get(1)).isEqualTo(2);

    map.clear();
    assertThat(map.get(1)).isNull();
  }


  @Test
  public void removeFromEmptyMap() {
    Map<Integer, Integer> map = freshMap();
    Integer removed = map.remove(null);

    assertThat(removed).isNull();

    removed = map.remove(1);

    assertThat(removed).isNull();
  }

  @Test
  public void removeFromNonEmptyMap() {
    Map<Integer, Integer> map = freshMap();
    map.put(1, 2);

    Integer removed = map.remove(null);
    assertThat(removed).isNull();

    removed = map.remove(1);
    assertThat(removed).isEqualTo(2);
  }

  @Test
  public void clear() {
    Map<Integer, Integer> map = freshMap();

    map.clear();
    assertThat(map).isEmpty();

    map.put(1, 2);
    assertThat(map).isNotEmpty();

    map.clear();
    assertThat(map).isEmpty();
  }

  @NotNull
  private Map<Integer, Integer> freshMap() {
    if ("jdk".equals(mapType)) {
      return new java.util.HashMap<>();
    }
    return new com.intellij.util.containers.HashMap<>();
  }
}