class IntervalsTree:
    def __init__(self, left = 0, right = 0, delta = 0):
        self.left = left
        self.right = right
        self.ar = [0] * (self.right - self.left)
        """
        self.delta = delta
        self.mid = (self.left + self.right) // 2

        if (self.right - self.left >= 2):
            self.left_subtree = IntervalsTree(self.left, self.mid)
            self.right_subtree = IntervalsTree(self.mid, self.right)
        else:
            self.left_subtree = None
            self.right_subtree = None
        """

    def add(self, left, right, delta):
        for i in xrange(max(left, self.left), min(right, self.right)):
            self.ar[i - self.left] += delta
        """
        if right <= left:
            return

        if (right <= self.left) or (left >= self.right):
            return 

        if (left <= self.left) and (self.right <= right):
            self.delta += delta
            return

        self.left_subtree.add(max(left, self.left), min(right, self.mid), delta)
        self.right_subtree.add(max(left, self.mid), min(right, self.right), delta)
        """


    def get(self, pos):
        if pos < self.left or pos >= self.right:
                return 0
        return self.ar[pos]
        """
        if (intervalsTree == None) or (pos < intervalsTree.left) or (pos >= intervalsTree.right):
            return 0

        if pos < intervalsTree.mid:
            return intervalsTree.delta + getValueAt(intervalsTree.left_subtree, pos)
        else:
            return intervalsTree.delta + getValueAt(intervalsTree.right_subtree, pos)
        """




