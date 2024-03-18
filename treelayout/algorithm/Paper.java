package treelayout.algorithm;

/* The extended Reingold-Tilford algorithm as described in the paper
 * "Drawing Non-layered Tidy Trees in Linear Time" by Atze van der Ploeg
 * Accepted for publication in Software: Practice and Experience, to Appear.
 */

public class Paper {
  public static class Tree {
    double width, height; // Width and height.
    double x, y, initialX, modifierX, shiftX, posChange;
    Tree threadLeft, threadRight; // Left and right thread.
    Tree extremeLeft, extremeRight; // Extreme left and right nodes.
    double modSumLeft, modSumRight; // Sum of modifiers at the extreme nodes.
    Tree[] children;

    Tree(double w, double h, double y, Tree... c) {
      this.width = w;
      this.height = h;
      this.y = y;
      this.children = c;
    }
  }

  static void layout(Tree tree) {
    firstWalk(tree);
    secondWalk(tree, 0);
  }

  static void firstWalk(Tree tree) {
    if (tree.children.length == 0) {
      setExtremes(tree);
      return;
    }
    firstWalk(tree.children[0]);
    // Create siblings in contour minimal vertical coordinate and
    // index list.
    IndexYLowest indexHighest = updateIndexYLowest(bottom(
        tree.children[0].extremeLeft), 0, null);
    for (int i = 1; i < tree.children.length; i++) {
      firstWalk(tree.children[i]);
      // Store lowest vertical coordinate while extreme nodes still
      // point in current subtree.
      double minY = bottom(tree.children[i].extremeRight);
      seperate(tree, i, indexHighest);
      indexHighest = updateIndexYLowest(minY, i, indexHighest);
    }
    positionRoot(tree);
    setExtremes(tree);
  }

  static void setExtremes(Tree tree) {
    if (tree.children.length == 0) {
      tree.extremeLeft = tree;
      tree.extremeRight = tree;
      tree.modSumLeft = tree.modSumRight = 0;
    } else {
      tree.extremeLeft = tree.children[0].extremeLeft;
      tree.modSumLeft = tree.children[0].modSumLeft;
      tree.extremeRight = tree.children[tree.children.length - 1].extremeRight;
      tree.modSumRight = tree.children[tree.children.length - 1].modSumRight;
    }
  }

  static void seperate(Tree tree, int i, IndexYLowest indexHighest) {
    // Right contour node of left siblings and its sum of modfiers.
    Tree subtreeRight = tree.children[i - 1];
    double modSumSubtreeRight = subtreeRight.modifierX;
    // Left contour node of current subtree and its sum of modfiers.
    Tree contourLeft = tree.children[i];
    double modSumContourLeft = contourLeft.modifierX;
    while (subtreeRight != null && contourLeft != null) {
      if (bottom(subtreeRight) > indexHighest.lowY) {
        indexHighest = indexHighest.next;
      }
      // How far to the left of the right side of sr is the left side of cl?
      double dist = (modSumSubtreeRight + subtreeRight.initialX + subtreeRight.width);
      dist = dist - (modSumContourLeft + contourLeft.initialX);
      if (dist > 0) {
        modSumContourLeft += dist;
        moveSubtree(tree, i, indexHighest.index, dist);
      }
      double subtreeY = bottom(subtreeRight);
      double contourY = bottom(contourLeft);
      // Advance highest node(s) and sum(s) of modifiers
      if (subtreeY <= contourY) {
        subtreeRight = nextRightContour(subtreeRight);
        if (subtreeRight != null) {
          modSumSubtreeRight += subtreeRight.modifierX;
        }
      }
      if (subtreeY >= contourY) {
        contourLeft = nextLeftContour(contourLeft);
        if (contourLeft != null) {
          modSumContourLeft += contourLeft.modifierX;
        }
      }
    }
    // Set threads and update extreme nodes.
    // In the first case, the current subtree must be taller than the left siblings.
    if (subtreeRight == null && contourLeft != null) {
      setLeftThread(tree, i, contourLeft, modSumContourLeft);
    }
    // In this case, the left siblings must be taller than the current subtree.
    else if (subtreeRight != null && contourLeft == null) {
      setRightThread(tree, i, subtreeRight, modSumSubtreeRight);
    }
  }

  static void moveSubtree(Tree tree, int i, int sourceIndex, double dist) {
    // Move subtree by changing mod.
    tree.children[i].modifierX += dist;
    tree.children[i].modSumLeft += dist;
    tree.children[i].modSumRight += dist;
    distributeExtra(tree, i, sourceIndex, dist);
  }

  static Tree nextLeftContour(Tree tree) {
    if (tree.children.length == 0) {
      return tree.threadLeft;
    } else {
      return tree.children[0];
    }
  }

  static Tree nextRightContour(Tree tree) {
    if (tree.children.length == 0) {
      return tree.threadRight;
    } else {
      return tree.children[tree.children.length - 1];
    }
  }

  static double bottom(Tree tree) {
    return tree.y + tree.height;
  }

  static void setLeftThread(Tree tree, int i, Tree contourLeft, double modSumContourLeft) {
    Tree leftIndex = tree.children[0].extremeLeft;
    leftIndex.threadLeft = contourLeft;
    // Change mod so that the sum of modifier after following thread is correct.
    double diff = (modSumContourLeft - contourLeft.modifierX);
    diff = diff - tree.children[0].modSumLeft;
    leftIndex.modifierX += diff;
    // Change preliminary x coordinate so that the node does not move.
    leftIndex.initialX -= diff;
    // Update extreme node and its sum of modifiers.
    tree.children[0].extremeLeft = tree.children[i].extremeLeft;
    tree.children[0].modSumLeft = tree.children[i].modSumLeft;
  }

  // Symmetrical to setLeftThread.
  static void setRightThread(Tree tree, int i, Tree subtreeRight, double modSumSubtreeRight) {
    Tree rightIndex = tree.children[i].extremeRight;
    rightIndex.threadRight = subtreeRight;
    double diff = (modSumSubtreeRight - subtreeRight.modifierX);
    diff = diff - tree.children[i].modSumRight;
    rightIndex.modifierX += diff;
    rightIndex.initialX -= diff;
    tree.children[i].extremeRight = tree.children[i - 1].extremeRight;
    tree.children[i].modSumRight = tree.children[i - 1].modSumRight;
  }

  static void positionRoot(Tree tree) {
    // Position root between children, taking into account their mod.
    tree.initialX = (tree.children[0].initialX +
        tree.children[0].modifierX +
        tree.children[tree.children.length - 1].modifierX +
        tree.children[tree.children.length - 1].initialX +
        tree.children[tree.children.length - 1].width) / 2 - tree.width / 2;
  }

  static void secondWalk(Tree tree, double modSum) {
    modSum += tree.modifierX;
    // Set absolute (non-relative) horizontal coordinate.
    tree.x = tree.initialX + modSum;
    addChildSpacing(tree);
    for (int i = 0; i < tree.children.length; i++) {
      secondWalk(tree.children[i], modSum);
    }
  }

  static void distributeExtra(Tree tree, int i, int sourceIndex, double dist) {
    // Are there intermediate children?
    if (sourceIndex != i - 1) {
      double numberRecipients = i - sourceIndex;
      tree.children[sourceIndex + 1].shiftX += dist / numberRecipients;
      tree.children[i].shiftX -= dist / numberRecipients;
      tree.children[i].posChange -= dist - dist / numberRecipients;
    }
  }

  // Process change and shift to add intermediate spacing to mod.
  static void addChildSpacing(Tree tree) {
    double d = 0, modSumDelta = 0;
    for (int i = 0; i < tree.children.length; i++) {
      d += tree.children[i].shiftX;
      modSumDelta += d + tree.children[i].posChange;
      tree.children[i].modifierX += modSumDelta;
    }
  }

  // A linked list of the indexes of left siblings and their 
  // lowest vertical coordinate.
  static class IndexYLowest {
    double lowY;
    int index;
    IndexYLowest next;

    public IndexYLowest(double lowY, int index, IndexYLowest next) {
      this.lowY = lowY;
      this.index = index;
      this.next = next;
    }
  }

  static IndexYLowest updateIndexYLowest(double minY, int i, IndexYLowest indexHighest) {
    // Remove siblings that are hidden by the new subtree.
    while (indexHighest != null && minY >= indexHighest.lowY) {
     indexHighest = indexHighest.next;
    }
    // Prepend the new subtree.
    return new IndexYLowest(minY, i, indexHighest);
  }
}
